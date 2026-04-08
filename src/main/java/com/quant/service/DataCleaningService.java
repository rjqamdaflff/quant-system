package com.quant.service;

import com.quant.entity.StockDaily;
import com.quant.repository.StockDailyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据清洗服务
 *
 * 提供股票数据的清洗、校验、处理功能：
 * 1. 停牌数据检测和标记
 * 2. 缺失值填充（前值填充、线性插值）
 * 3. 异常值检测和处理
 * 4. 复权计算（前复权）
 * 5. 数据完整性检查
 *
 * 所有的清洗操作都保留原始数据，通过标记字段区分数据状态
 */
@Service
public class DataCleaningService {

    private static final Logger logger = LoggerFactory.getLogger(DataCleaningService.class);

    /** 停牌标记：成交量阈值，低于此值视为停牌 */
    private static final Long SUSPENSION_VOLUME_THRESHOLD = 0L;

    /** 异常涨跌幅阈值（百分比），超过此值需人工确认 */
    private static final BigDecimal ABNORMAL_PCT_THRESHOLD = new BigDecimal("11.0");

    /** ST股票涨跌幅阈值 */
    private static final BigDecimal ST_PCT_THRESHOLD = new BigDecimal("5.0");

    private final StockDailyRepository stockDailyRepository;
    private final DataValidationService validationService;

    public DataCleaningService(StockDailyRepository stockDailyRepository,
                               DataValidationService validationService) {
        this.stockDailyRepository = stockDailyRepository;
        this.validationService = validationService;
    }

    /**
     * 清洗指定股票的历史数据
     *
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 清洗结果报告
     */
    public CleaningReport cleanStockData(String stockCode, LocalDate startDate, LocalDate endDate) {
        logger.info("开始清洗股票数据: {}, 日期范围: {} - {}", stockCode, startDate, endDate);

        CleaningReport report = new CleaningReport(stockCode);

        // 获取原始数据
        List<StockDaily> dailyList = stockDailyRepository.findByStockCodeAndTradeDateBetween(
            stockCode, startDate, endDate
        );

        if (dailyList.isEmpty()) {
            logger.warn("股票 {} 在指定时间范围内无数据", stockCode);
            return report;
        }

        List<StockDaily> cleanedList = new ArrayList<>();
        StockDaily previousDay = null;

        for (StockDaily daily : dailyList) {
            // 1. 检测停牌
            if (isSuspension(daily)) {
                daily.setDataStatus("SUSPENDED");
                report.incrementSuspensionCount();
                // 停牌数据不删除，标记后保留
            }

            // 2. 检测缺失值
            if (hasMissingValues(daily)) {
                report.incrementMissingCount();
                if (previousDay != null) {
                    // 前值填充
                    fillMissingValues(daily, previousDay);
                    daily.setDataStatus("FILLED");
                    report.incrementFilledCount();
                }
            }

            // 3. 检测异常值
            if (hasAnomalies(daily, previousDay)) {
                daily.setDataStatus("ABNORMAL");
                report.incrementAbnormalCount();
                logger.warn("检测到异常数据: {} 日期: {}", stockCode, daily.getTradeDate());
            }

            // 4. 价格逻辑校验
            DataValidationService.ValidationResult validationResult = validationService.validateSingle(daily);
            if (!validationResult.isValid()) {
                daily.setDataStatus("INVALID");
                report.incrementInvalidCount();
            }

            cleanedList.add(daily);
            previousDay = daily;
        }

        // 保存清洗后的数据
        stockDailyRepository.saveAll(cleanedList);

        logger.info("数据清洗完成: {}", report);
        return report;
    }

    /**
     * 检测是否停牌
     * 停牌判断标准：成交量 = 0
     */
    private boolean isSuspension(StockDaily daily) {
        return daily.getVolume() == null || daily.getVolume() <= SUSPENSION_VOLUME_THRESHOLD;
    }

    /**
     * 检测是否存在缺失值
     */
    private boolean hasMissingValues(StockDaily daily) {
        return daily.getOpen() == null ||
               daily.getClose() == null ||
               daily.getHigh() == null ||
               daily.getLow() == null;
    }

    /**
     * 使用前值填充缺失数据
     */
    private void fillMissingValues(StockDaily current, StockDaily previous) {
        if (current.getOpen() == null) {
            current.setOpen(previous.getClose());
        }
        if (current.getClose() == null) {
            current.setClose(previous.getClose());
        }
        if (current.getHigh() == null) {
            current.setHigh(current.getClose());
        }
        if (current.getLow() == null) {
            current.setLow(current.getClose());
        }
        if (current.getVolume() == null) {
            current.setVolume(0L);
        }
    }

    /**
     * 检测是否存在异常值
     */
    private boolean hasAnomalies(StockDaily daily, StockDaily previous) {
        if (previous == null || daily.getChangePct() == null) {
            return false;
        }

        BigDecimal absPctChg = daily.getChangePct().abs();

        // 涨跌幅超过阈值
        if (absPctChg.compareTo(ABNORMAL_PCT_THRESHOLD) > 0) {
            // 非ST股票涨跌幅超过11%视为异常
            return true;
        }

        return false;
    }

    /**
     * 计算前复权价格
     *
     * @param stockCode 股票代码
     * @param adjustFactor 复权因子（除权除息比例）
     */
    public void calculateAdjustmentFactor(String stockCode, BigDecimal adjustFactor) {
        logger.info("计算复权因子: {}, 因子: {}", stockCode, adjustFactor);

        List<StockDaily> dailyList = stockDailyRepository.findByStockCodeOrderByTradeDateAsc(stockCode);

        for (StockDaily daily : dailyList) {
            if (daily.getClose() != null && adjustFactor != null) {
                BigDecimal adjustedClose = daily.getClose().multiply(adjustFactor)
                    .setScale(3, RoundingMode.HALF_UP);
                daily.setAdjPrice(adjustedClose);
                daily.setDataStatus("ADJUSTED");
            }
        }

        stockDailyRepository.saveAll(dailyList);
        logger.info("复权计算完成: {} 条记录", dailyList.size());
    }

    /**
     * 数据完整性检查
     *
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 完整性检查报告
     */
    public IntegrityReport checkDataIntegrity(String stockCode, LocalDate startDate, LocalDate endDate) {
        IntegrityReport report = new IntegrityReport(stockCode);

        List<StockDaily> dailyList = stockDailyRepository.findByStockCodeAndTradeDateBetween(
            stockCode, startDate, endDate
        );

        report.setTotalRecords(dailyList.size());

        for (StockDaily daily : dailyList) {
            // 检查必填字段
            if (daily.getOpen() == null) report.incrementMissingFields();
            if (daily.getClose() == null) report.incrementMissingFields();
            if (daily.getHigh() == null) report.incrementMissingFields();
            if (daily.getLow() == null) report.incrementMissingFields();
            if (daily.getVolume() == null) report.incrementMissingFields();

            // 检查日期连续性
            // TODO: 实现交易日历检查
        }

        report.calculateIntegrityScore();
        return report;
    }

    /**
     * 批量清洗多只股票数据
     */
    public List<CleaningReport> batchCleanStockData(List<String> stockCodes,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        List<CleaningReport> reports = new ArrayList<>();

        for (String stockCode : stockCodes) {
            try {
                CleaningReport report = cleanStockData(stockCode, startDate, endDate);
                reports.add(report);
            } catch (Exception e) {
                logger.error("清洗股票 {} 数据失败: {}", stockCode, e.getMessage());
            }
        }

        return reports;
    }

    // ========== 内部类：清洗报告 ==========

    /**
     * 数据清洗报告
     */
    public static class CleaningReport {
        private final String stockCode;
        private int totalCount;
        private int suspensionCount;
        private int missingCount;
        private int filledCount;
        private int abnormalCount;
        private int invalidCount;

        public CleaningReport(String stockCode) {
            this.stockCode = stockCode;
        }

        public void incrementSuspensionCount() { suspensionCount++; }
        public void incrementMissingCount() { missingCount++; }
        public void incrementFilledCount() { filledCount++; }
        public void incrementAbnormalCount() { abnormalCount++; }
        public void incrementInvalidCount() { invalidCount++; }

        // Getters
        public String getStockCode() { return stockCode; }
        public int getTotalCount() { return totalCount; }
        public int getSuspensionCount() { return suspensionCount; }
        public int getMissingCount() { return missingCount; }
        public int getFilledCount() { return filledCount; }
        public int getAbnormalCount() { return abnormalCount; }
        public int getInvalidCount() { return invalidCount; }

        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        @Override
        public String toString() {
            return String.format(
                "CleaningReport{stockCode='%s', total=%d, suspension=%d, missing=%d, filled=%d, abnormal=%d, invalid=%d}",
                stockCode, totalCount, suspensionCount, missingCount, filledCount, abnormalCount, invalidCount
            );
        }
    }

    /**
     * 数据完整性报告
     */
    public static class IntegrityReport {
        private final String stockCode;
        private int totalRecords;
        private int missingFields;
        private double integrityScore;

        public IntegrityReport(String stockCode) {
            this.stockCode = stockCode;
        }

        public void incrementMissingFields() { missingFields++; }

        public void calculateIntegrityScore() {
            if (totalRecords == 0) {
                integrityScore = 0;
            } else {
                int expectedFields = totalRecords * 5; // 5个必填字段
                integrityScore = 100.0 * (expectedFields - missingFields) / expectedFields;
            }
        }

        // Getters and Setters
        public String getStockCode() { return stockCode; }
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        public int getMissingFields() { return missingFields; }
        public double getIntegrityScore() { return integrityScore; }
    }
}