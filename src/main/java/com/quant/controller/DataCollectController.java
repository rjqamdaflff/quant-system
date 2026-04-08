package com.quant.controller;

import com.quant.entity.StockDaily;
import com.quant.entity.StockInfo;
import com.quant.repository.StockDailyRepository;
import com.quant.repository.StockInfoRepository;
import com.quant.service.StockDataCollectorService;
import com.quant.service.StockDataCollectorService.BatchCollectResult;
import com.quant.service.StockDataCollectorService.CollectResult;
import com.quant.service.DataValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据采集控制器
 *
 * <p>提供数据采集相关的 REST API 接口</p>
 *
 * <p>接口列表：</p>
 * <table border="1">
 *   <tr><th>接口</th><th>方法</th><th>说明</th></tr>
 *   <tr><td>/api/collect/stocks</td><td>GET</td><td>获取股票列表</td></tr>
 *   <tr><td>/api/collect/status</td><td>GET</td><td>获取系统状态</td></tr>
 *   <tr><td>/api/collect/daily/{code}</td><td>GET</td><td>查询日线数据</td></tr>
 *   <tr><td>/api/collect/daily/{code}</td><td>POST</td><td>采集单只股票</td></tr>
 *   <tr><td>/api/collect/daily/batch</td><td>POST</td><td>批量采集</td></tr>
 *   <tr><td>/api/collect/validate/{code}</td><td>GET</td><td>验证数据</td></tr>
 * </table>
 *
 * @author quant-system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/collect")
@RequiredArgsConstructor
public class DataCollectController {

    /** 数据采集服务 */
    private final StockDataCollectorService collectorService;

    /** 数据验证服务 */
    private final DataValidationService validationService;

    /** 股票信息仓库 */
    private final StockInfoRepository stockInfoRepository;

    /** 日线数据仓库 */
    private final StockDailyRepository stockDailyRepository;

    /**
     * 获取股票列表
     *
     * <p>从数据源获取所有A股股票的基本信息</p>
     *
     * @return 股票列表，包含总数和股票详情
     */
    @GetMapping("/stocks")
    public ResponseEntity<Map<String, Object>> getStockList() {
        List<StockInfo> stocks = collectorService.getAllStockList();
        Map<String, Object> result = new HashMap<>();
        result.put("total", stocks.size());
        result.put("stocks", stocks);
        return ResponseEntity.ok(result);
    }

    /**
     * 采集单只股票日线数据
     *
     * <p>从数据源采集指定股票的日线数据</p>
     *
     * @param stockCode 股票代码（如：000001）
     * @param startDate 开始日期（可选，默认一个月前）
     * @param endDate 结束日期（可选，默认今天）
     * @return 采集结果
     */
    @PostMapping("/daily/{stockCode}")
    public ResponseEntity<Map<String, Object>> collectDaily(
            @PathVariable String stockCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        // 默认日期范围：最近一个月
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        CollectResult result = collectorService.collectDailyData(stockCode, startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("stockCode", stockCode);
        response.put("recordCount", result.count());
        response.put("message", result.success() ?
            "采集成功" : "采集失败: " + result.errorMsg());

        return ResponseEntity.ok(response);
    }

    /**
     * 批量采集所有股票
     *
     * <p>遍历股票列表，逐个采集日线数据</p>
     *
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 批量采集结果
     */
    @PostMapping("/daily/batch")
    public ResponseEntity<Map<String, Object>> collectAllDaily(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        // 默认日期范围
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        log.info("开始批量采集，日期范围: {} ~ {}", startDate, endDate);

        BatchCollectResult result = collectorService.collectAllStocksDaily(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("total", result.total());
        response.put("success", result.success());
        response.put("failed", result.failed());
        response.put("successRate", String.format("%.2f%%", result.getSuccessRate()));
        response.put("failedStocks", result.failedStocks());

        return ResponseEntity.ok(response);
    }

    /**
     * 查询股票日线数据
     *
     * <p>从数据库查询已采集的日线数据</p>
     *
     * @param stockCode 股票代码
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 日线数据列表
     */
    @GetMapping("/daily/{stockCode}")
    public ResponseEntity<Map<String, Object>> getDailyData(
            @PathVariable String stockCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        List<StockDaily> dailyList;
        if (startDate != null && endDate != null) {
            dailyList = stockDailyRepository.findByStockCodeAndTradeDateBetween(stockCode, startDate, endDate);
        } else {
            // 默认返回最近30条
            dailyList = stockDailyRepository.findRecentByStockCode(stockCode, 30);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("stockCode", stockCode);
        response.put("total", dailyList.size());
        response.put("data", dailyList);

        return ResponseEntity.ok(response);
    }

    /**
     * 验证数据准确性
     *
     * <p>检查指定股票的数据完整性，统计异常数据</p>
     *
     * @param stockCode 股票代码
     * @return 数据验证报告
     */
    @GetMapping("/validate/{stockCode}")
    public ResponseEntity<Map<String, Object>> validateData(@PathVariable String stockCode) {
        List<StockDaily> dailyList = stockDailyRepository.findByStockCodeOrderByTradeDateDesc(stockCode);

        // 数据验证统计
        int valid = 0, invalid = 0;
        for (StockDaily daily : dailyList) {
            if (validationService.validateSingle(daily).isValid()) {
                valid++;
            } else {
                invalid++;
            }
        }

        // 异常数据检查
        List<StockDaily> abnormalChange = stockDailyRepository.findAbnormalChangePct();
        List<StockDaily> negativeVolume = stockDailyRepository.findNegativeVolume();
        List<StockDaily> invalidPrices = stockDailyRepository.findInvalidPrices();

        Map<String, Object> response = new HashMap<>();
        response.put("stockCode", stockCode);
        response.put("totalRecords", dailyList.size());
        response.put("validRecords", valid);
        response.put("invalidRecords", invalid);
        response.put("abnormalChangeRecords", abnormalChange.size());
        response.put("negativeVolumeRecords", negativeVolume.size());
        response.put("invalidPriceRecords", invalidPrices.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取系统状态
     *
     * <p>返回当前系统的数据统计信息</p>
     *
     * @return 系统状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        long stockCount = stockInfoRepository.count();
        long dailyCount = stockDailyRepository.count();

        Map<String, Object> response = new HashMap<>();
        response.put("stockCount", stockCount);
        response.put("dailyRecordCount", dailyCount);
        response.put("status", "running");
        response.put("module", "Data Collection Layer");

        return ResponseEntity.ok(response);
    }
}