package com.quant.controller;

import com.quant.entity.BacktestResult;
import com.quant.entity.SignalAlert;
import com.quant.entity.StrategyConfig;
import com.quant.repository.StrategyConfigRepository;
import com.quant.service.BacktestService;
import com.quant.service.IndicatorCalculator;
import com.quant.service.SignalGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 策略分析控制器
 *
 * 提供策略分析相关的REST API：
 * - 技术指标计算
 * - 策略配置管理
 * - 回测执行
 */
@RestController
@RequestMapping("/api/strategy")
public class StrategyController {

    private final BacktestService backtestService;
    private final SignalGenerator signalGenerator;
    private final StrategyConfigRepository strategyConfigRepository;

    public StrategyController(BacktestService backtestService,
                              SignalGenerator signalGenerator,
                              StrategyConfigRepository strategyConfigRepository) {
        this.backtestService = backtestService;
        this.signalGenerator = signalGenerator;
        this.strategyConfigRepository = strategyConfigRepository;
    }

    /**
     * 执行回测
     *
     * @param request 回测请求参数
     * @return 回测结果
     */
    @PostMapping("/backtest")
    public ResponseEntity<BacktestResult> runBacktest(@RequestBody BacktestRequest request) {
        BacktestResult result = backtestService.runBacktest(
            request.getStockCode(),
            request.getStartDate(),
            request.getEndDate(),
            request.getInitialCapital() != null ? request.getInitialCapital() : new BigDecimal("100000"),
            request.getStrategyType() != null ? request.getStrategyType() : "MA_GOLDEN_CROSS"
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 获取回测结果列表
     *
     * @param stockCode 股票代码
     * @return 回测结果列表
     */
    @GetMapping("/backtest/{stockCode}")
    public ResponseEntity<List<BacktestResult>> getBacktestResults(@PathVariable String stockCode) {
        List<BacktestResult> results = backtestService.getBacktestResults(stockCode);
        return ResponseEntity.ok(results);
    }

    /**
     * 获取策略配置列表
     *
     * @return 策略配置列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<StrategyConfig>> getStrategyList() {
        List<StrategyConfig> strategies = strategyConfigRepository.findAll();
        return ResponseEntity.ok(strategies);
    }

    /**
     * 创建策略配置
     *
     * @param config 策略配置
     * @return 保存后的配置
     */
    @PostMapping("/config")
    public ResponseEntity<StrategyConfig> createStrategyConfig(@RequestBody StrategyConfig config) {
        config.setCreateTime(java.time.LocalDateTime.now());
        config.setUpdateTime(java.time.LocalDateTime.now());
        config.setStatus("ACTIVE");
        StrategyConfig saved = strategyConfigRepository.save(config);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新策略配置
     *
     * @param id 策略ID
     * @param config 更新的配置
     * @return 更新后的配置
     */
    @PutMapping("/config/{id}")
    public ResponseEntity<StrategyConfig> updateStrategyConfig(
            @PathVariable Long id,
            @RequestBody StrategyConfig config) {
        return strategyConfigRepository.findById(id)
            .map(existing -> {
                existing.setStrategyName(config.getStrategyName());
                existing.setStrategyType(config.getStrategyType());
                existing.setTargetStocks(config.getTargetStocks());
                existing.setStrategyParams(config.getStrategyParams());
                existing.setBuyCondition(config.getBuyCondition());
                existing.setSellCondition(config.getSellCondition());
                existing.setStopLossPct(config.getStopLossPct());
                existing.setStopProfitPct(config.getStopProfitPct());
                existing.setAlertEnabled(config.getAlertEnabled());
                existing.setAlertChannels(config.getAlertChannels());
                existing.setUpdateTime(java.time.LocalDateTime.now());
                return ResponseEntity.ok(strategyConfigRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除策略配置
     *
     * @param id 策略ID
     * @return 删除结果
     */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<Void> deleteStrategyConfig(@PathVariable Long id) {
        if (strategyConfigRepository.existsById(id)) {
            strategyConfigRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 获取可用策略类型
     *
     * @return 策略类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<List<Map<String, String>>> getStrategyTypes() {
        List<Map<String, String>> types = List.of(
            Map.of("code", "MA_GOLDEN_CROSS", "name", "均线金叉策略", "desc", "MA5上穿MA20买入，下穿卖出"),
            Map.of("code", "MACD_CROSS", "name", "MACD交叉策略", "desc", "DIF上穿DEA买入，下穿卖出"),
            Map.of("code", "KDJ_OVERSOLD", "name", "KDJ超卖策略", "desc", "J值低于20买入，高于80卖出"),
            Map.of("code", "BOLL_BAND", "name", "布林带策略", "desc", "触及下轨买入，触及上轨卖出")
        );
        return ResponseEntity.ok(types);
    }

    // ========== 内部类：请求参数 ==========

    /**
     * 回测请求参数
     */
    public static class BacktestRequest {
        private String stockCode;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal initialCapital;
        private String strategyType;

        // Getters and Setters
        public String getStockCode() { return stockCode; }
        public void setStockCode(String stockCode) { this.stockCode = stockCode; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getInitialCapital() { return initialCapital; }
        public void setInitialCapital(BigDecimal initialCapital) { this.initialCapital = initialCapital; }
        public String getStrategyType() { return strategyType; }
        public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    }
}