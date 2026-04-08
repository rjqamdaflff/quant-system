package com.quant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.entity.StockDaily;
import com.quant.entity.StockInfo;
import com.quant.repository.StockDailyRepository;
import com.quant.repository.StockInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 股票数据采集服务
 *
 * <p>功能说明：</p>
 * <ul>
 *   <li>通过Python脚本调用AKShare/Baostock获取真实数据</li>
 *   <li>支持单只股票采集和批量采集</li>
 *   <li>支持增量采集（断点续传）</li>
 *   <li>网络异常时自动重试</li>
 * </ul>
 *
 * <p>重要规则：</p>
 * <ul>
 *   <li>禁止使用模拟数据，必须获取真实准确的数据</li>
 *   <li>网络不可用时抛出异常，不返回假数据</li>
 *   <li>所有入库数据必须经过验证</li>
 * </ul>
 *
 * @author quant-system
 * @version 1.0.0
 */
@Slf4j
@Service
public class StockDataCollectorService {

    /** 股票基本信息仓库 */
    private final StockInfoRepository stockInfoRepository;

    /** 日线数据仓库 */
    private final StockDailyRepository stockDailyRepository;

    /** 数据验证服务 */
    private final DataValidationService validationService;

    /** 采集日志服务 */
    private final CollectLogService collectLogService;

    /** JSON解析器 */
    private final ObjectMapper objectMapper;

    /** 重试次数，默认3次 */
    @Value("${quant.collect.retry-times:3}")
    private int retryTimes;

    /** Python脚本路径 */
    @Value("${quant.collect.script-path:scripts/data_collector.py}")
    private String scriptPath;

    /** Python解释器路径 */
    @Value("${quant.collect.python-path:python3}")
    private String pythonPath;

    /**
     * 构造函数 - 依赖注入
     *
     * @param stockInfoRepository 股票信息仓库
     * @param stockDailyRepository 日线数据仓库
     * @param validationService 数据验证服务
     * @param collectLogService 采集日志服务
     */
    public StockDataCollectorService(
            StockInfoRepository stockInfoRepository,
            StockDailyRepository stockDailyRepository,
            DataValidationService validationService,
            CollectLogService collectLogService) {
        this.stockInfoRepository = stockInfoRepository;
        this.stockDailyRepository = stockDailyRepository;
        this.validationService = validationService;
        this.collectLogService = collectLogService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 测试数据源连接状态
     *
     * <p>检测AKShare和Baostock数据源是否可用</p>
     *
     * @return 数据源状态对象，包含两个数据源的可用性
     */
    public DataSourceStatus testDataSourceConnection() {
        try {
            String output = executePythonScript("test");
            JsonNode result = objectMapper.readTree(output);

            return new DataSourceStatus(
                result.get("akshare").asBoolean(),
                result.get("baostock").asBoolean(),
                result.get("message").asText()
            );
        } catch (Exception e) {
            log.error("测试数据源连接失败", e);
            return new DataSourceStatus(false, false, "连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有股票列表 - 从真实数据源
     *
     * <p>调用Python脚本获取A股所有股票的基本信息</p>
     * <p>包括：股票代码、股票名称、所属市场（沪市/深市）</p>
     *
     * @return 股票信息列表
     * @throws RuntimeException 当数据源不可用时抛出异常
     */
    @Transactional
    public List<StockInfo> getAllStockList() {
        log.info("从数据源获取股票列表...");

        try {
            String output = executePythonScript("stock_list");
            JsonNode result = objectMapper.readTree(output);

            if (!result.get("success").asBoolean()) {
                String error = result.get("message").asText();
                log.error("获取股票列表失败: {}", error);
                collectLogService.logError("stock_list", null, "获取失败: " + error);
                throw new RuntimeException("无法获取股票列表: " + error);
            }

            List<StockInfo> stockList = new ArrayList<>();
            JsonNode data = result.get("data");

            for (JsonNode item : data) {
                StockInfo info = StockInfo.builder()
                    .stockCode(item.get("stock_code").asText())
                    .stockName(item.get("stock_name").asText())
                    .market(item.get("market").asText())
                    .build();
                stockList.add(info);
            }

            log.info("成功获取 {} 只股票", stockList.size());
            return stockList;

        } catch (Exception e) {
            log.error("获取股票列表异常", e);
            collectLogService.logError("stock_list", null, e.getMessage());
            throw new RuntimeException("获取股票列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 采集单只股票日线数据 - 从真实数据源
     *
     * <p>采集流程：</p>
     * <ol>
     *   <li>调用Python脚本获取原始数据</li>
     *   <li>解析JSON格式数据</li>
     *   <li>数据验证和清洗</li>
     *   <li>保存到数据库</li>
     *   <li>记录采集日志</li>
     * </ol>
     *
     * @param stockCode 股票代码（如：000001）
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @return 采集结果对象，包含成功/失败状态和记录数
     */
    @Transactional
    public CollectResult collectDailyData(String stockCode, LocalDate startDate, LocalDate endDate) {
        log.info("采集股票 {} 日线数据，日期范围: {} ~ {}", stockCode, startDate, endDate);

        LocalDateTime startTime = LocalDateTime.now();
        int retries = 0;
        int maxRetries = retryTimes > 0 ? retryTimes : 3;
        Exception lastError = null;

        // 重试循环
        while (retries < maxRetries) {
            try {
                // 调用Python脚本获取真实数据
                String output = executePythonScript(
                    "daily",
                    "--code", stockCode,
                    "--start", startDate.toString(),
                    "--end", endDate.toString()
                );

                JsonNode result = objectMapper.readTree(output);

                if (!result.get("success").asBoolean()) {
                    String error = result.get("message").asText();
                    log.warn("股票 {} 数据获取失败: {}", stockCode, error);
                    return CollectResult.failed(stockCode, "数据源返回失败: " + error);
                }

                // 解析日线数据
                List<StockDaily> dailyData = parseDailyData(result.get("data"), stockCode);

                if (dailyData.isEmpty()) {
                    log.warn("股票 {} 无数据返回", stockCode);
                    return CollectResult.failed(stockCode, "无数据返回");
                }

                // 数据验证和清洗
                List<StockDaily> validData = validationService.validateAndClean(dailyData);

                if (validData.isEmpty()) {
                    log.warn("股票 {} 数据验证后无有效数据", stockCode);
                    return CollectResult.failed(stockCode, "数据验证失败，无有效数据");
                }

                // 保存到数据库
                int savedCount = saveDailyData(validData);

                // 记录成功日志
                LocalDateTime endTime = LocalDateTime.now();
                collectLogService.logSuccess("daily", stockCode, startTime, endTime, savedCount);

                log.info("股票 {} 采集完成，有效数据 {} 条，保存 {} 条", stockCode, validData.size(), savedCount);
                return CollectResult.success(stockCode, savedCount);

            } catch (Exception e) {
                lastError = e;
                retries++;
                log.warn("股票 {} 采集失败，第 {} 次重试。错误: {}", stockCode, retries, e.getMessage());
            }
        }

        // 所有重试都失败，记录错误日志
        String errorMsg = lastError != null ? lastError.getMessage() : "未知错误";
        collectLogService.logError("daily", stockCode, "采集失败: " + errorMsg);
        return CollectResult.failed(stockCode, errorMsg);
    }

    /**
     * 解析日线数据
     *
     * <p>将JSON格式的数据转换为StockDaily实体对象</p>
     *
     * @param dataArray JSON数组节点
     * @param stockCode 股票代码
     * @return 日线数据列表
     */
    private List<StockDaily> parseDailyData(JsonNode dataArray, String stockCode) {
        List<StockDaily> list = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (JsonNode item : dataArray) {
            try {
                StockDaily daily = StockDaily.builder()
                    .stockCode(stockCode)
                    .tradeDate(LocalDate.parse(item.get("trade_date").asText(), formatter))
                    .open(item.has("open") ? new BigDecimal(item.get("open").asText()) : null)
                    .close(item.has("close") ? new BigDecimal(item.get("close").asText()) : null)
                    .high(item.has("high") ? new BigDecimal(item.get("high").asText()) : null)
                    .low(item.has("low") ? new BigDecimal(item.get("low").asText()) : null)
                    .volume(item.has("volume") ? item.get("volume").asLong() : null)
                    .amount(item.has("amount") ? new BigDecimal(item.get("amount").asText()) : null)
                    .changePct(item.has("change_pct") ? new BigDecimal(item.get("change_pct").asText()) : null)
                    .build();
                list.add(daily);
            } catch (Exception e) {
                log.warn("解析数据失败: {}", e.getMessage());
            }
        }

        return list;
    }

    /**
     * 执行Python脚本
     *
     * <p>通过ProcessBuilder调用外部Python脚本</p>
     *
     * @param args 命令行参数
     * @return 脚本输出内容
     * @throws Exception 当脚本执行失败时抛出异常
     */
    private String executePythonScript(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(scriptPath);
        command.addAll(Arrays.asList(args));

        log.debug("执行命令: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 读取脚本输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python脚本执行失败，退出码: " + exitCode + ", 输出: " + output);
        }

        return output.toString();
    }

    /**
     * 保存日线数据到数据库
     *
     * <p>自动去重：如果数据已存在则跳过</p>
     *
     * @param dailyList 日线数据列表
     * @return 实际保存的记录数
     */
    @Transactional
    public int saveDailyData(List<StockDaily> dailyList) {
        int count = 0;
        for (StockDaily daily : dailyList) {
            // 检查是否已存在
            if (!stockDailyRepository.existsByStockCodeAndTradeDate(daily.getStockCode(), daily.getTradeDate())) {
                stockDailyRepository.save(daily);
                count++;
            }
        }
        return count;
    }

    /**
     * 批量采集所有股票数据
     *
     * <p>遍历股票列表，逐个采集日线数据</p>
     * <p>支持进度显示和失败记录</p>
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 批量采集结果，包含总数、成功数、失败数
     */
    public BatchCollectResult collectAllStocksDaily(LocalDate startDate, LocalDate endDate) {
        log.info("开始批量采集所有股票日线数据...");

        List<StockInfo> stockList = getAllStockList();
        int total = stockList.size();
        int success = 0;
        int failed = 0;
        List<String> failedStocks = new ArrayList<>();

        for (StockInfo stock : stockList) {
            try {
                CollectResult result = collectDailyData(stock.getStockCode(), startDate, endDate);
                if (result.success()) {
                    success++;
                } else {
                    failed++;
                    failedStocks.add(stock.getStockCode() + ": " + result.errorMsg());
                }
            } catch (Exception e) {
                failed++;
                failedStocks.add(stock.getStockCode() + ": " + e.getMessage());
            }

            // 进度日志（每10只股票记录一次）
            if ((success + failed) % 10 == 0) {
                log.info("采集进度: {}/{} 成功, {} 失败", success + failed, total, failed);
            }
        }

        log.info("批量采集完成: 总数 {}, 成功 {}, 失败 {}", total, success, failed);
        return new BatchCollectResult(total, success, failed, failedStocks);
    }

    /**
     * 增量采集（断点续传）
     *
     * <p>从数据库中查询该股票最新的交易日期，只采集之后的数据</p>
     * <p>如果数据已是最新，返回成功但记录数为0</p>
     *
     * @param stockCode 股票代码
     * @return 采集结果
     */
    public CollectResult incrementalCollect(String stockCode) {
        // 查询最新交易日期
        Optional<LocalDate> latestDate = stockDailyRepository.findLatestTradeDateByStockCode(stockCode);

        LocalDate startDate = latestDate.orElse(LocalDate.now().minusYears(1)).plusDays(1);
        LocalDate endDate = LocalDate.now();

        // 如果开始日期在结束日期之后，说明数据已是最新
        if (startDate.isAfter(endDate)) {
            log.info("股票 {} 数据已是最新，无需采集", stockCode);
            return CollectResult.success(stockCode, 0);
        }

        return collectDailyData(stockCode, startDate, endDate);
    }

    // ==================== 内部类 ====================

    /**
     * 采集结果记录类
     *
     * @param success 是否成功
     * @param stockCode 股票代码
     * @param count 采集记录数
     * @param errorMsg 错误信息（失败时）
     */
    public record CollectResult(boolean success, String stockCode, int count, String errorMsg) {

        /**
         * 创建成功结果
         */
        public static CollectResult success(String stockCode, int count) {
            return new CollectResult(true, stockCode, count, null);
        }

        /**
         * 创建失败结果
         */
        public static CollectResult failed(String stockCode, String errorMsg) {
            return new CollectResult(false, stockCode, 0, errorMsg);
        }
    }

    /**
     * 批量采集结果记录类
     */
    public record BatchCollectResult(int total, int success, int failed, List<String> failedStocks) {

        /**
         * 是否全部成功
         */
        public boolean isCompleteSuccess() {
            return failed == 0;
        }

        /**
         * 计算成功率（百分比）
         */
        public double getSuccessRate() {
            return total > 0 ? (double) success / total * 100 : 0;
        }
    }

    /**
     * 数据源状态记录类
     */
    public record DataSourceStatus(boolean akshareAvailable, boolean baostockAvailable, String message) {}
}