package com.quant.service;

import com.quant.entity.BacktestResult;
import com.quant.entity.StockDaily;
import com.quant.entity.StrategyConfig;
import com.quant.repository.BacktestResultRepository;
import com.quant.repository.StockDailyRepository;
import com.quant.repository.StrategyConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 策略回测服务
 *
 * 提供策略回测的核心功能：
 * 1. 执行策略回测
 * 2. 计算收益指标（总收益、年化收益、最大回撤、夏普比率）
 * 3. 生成交易记录
 * 4. 保存回测结果
 *
 * 回测基于历史真实数据，不使用模拟数据
 */
@Service
public class BacktestService {

    private static final Logger logger = LoggerFactory.getLogger(BacktestService.class);

    /** 无风险收益率（年化，用于计算夏普比率） */
    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.03");

    /** 一年的交易日数量 */
    private static final int TRADING_DAYS_PER_YEAR = 250;

    private final BacktestResultRepository backtestResultRepository;
    private final StockDailyRepository stockDailyRepository;
    private final StrategyConfigRepository strategyConfigRepository;
    private final IndicatorCalculator indicatorCalculator;

    public BacktestService(BacktestResultRepository backtestResultRepository,
                           StockDailyRepository stockDailyRepository,
                           StrategyConfigRepository strategyConfigRepository,
                           IndicatorCalculator indicatorCalculator) {
        this.backtestResultRepository = backtestResultRepository;
        this.stockDailyRepository = stockDailyRepository;
        this.strategyConfigRepository = strategyConfigRepository;
        this.indicatorCalculator = indicatorCalculator;
    }

    /**
     * 执行策略回测
     *
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param initialCapital 初始资金
     * @param strategyType 策略类型
     * @return 回测结果
     */
    public BacktestResult runBacktest(String stockCode,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       BigDecimal initialCapital,
                                       String strategyType) {
        logger.info("开始回测: 股票={}, 策略={}, 时间范围={}~{}",
            stockCode, strategyType, startDate, endDate);

        // 创建回测结果对象
        BacktestResult result = new BacktestResult();
        result.setStockCode(stockCode);
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        result.setInitialCapital(initialCapital);
        result.setBacktestTime(LocalDateTime.now());
        result.setBacktestStatus("RUNNING");

        try {
            // 获取历史数据
            List<StockDaily> dailyList = stockDailyRepository
                .findByStockCodeAndTradeDateBetween(stockCode, startDate, endDate);

            if (dailyList.isEmpty()) {
                result.setBacktestStatus("FAILED");
                result.setErrorMsg("未找到历史数据");
                return result;
            }

            // 执行策略交易
            List<TradeRecord> trades = executeStrategy(dailyList, strategyType);

            // 计算收益指标
            calculatePerformanceMetrics(result, dailyList, trades, initialCapital);

            // 保存交易记录
            result.setTradeRecords(convertTradesToJson(trades));
            result.setBacktestStatus("COMPLETED");

            logger.info("回测完成: 总收益率={}%, 年化收益率={}%, 最大回撤={}%",
                result.getTotalReturn(),
                result.getAnnualReturn(),
                result.getMaxDrawdown());

        } catch (Exception e) {
            logger.error("回测执行失败: {}", e.getMessage(), e);
            result.setBacktestStatus("FAILED");
            result.setErrorMsg(e.getMessage());
        }

        return backtestResultRepository.save(result);
    }

    /**
     * 执行策略生成交易信号
     */
    private List<TradeRecord> executeStrategy(List<StockDaily> dailyList, String strategyType) {
        List<TradeRecord> trades = new ArrayList<>();

        // 计算技术指标
        BigDecimal[] ma5 = indicatorCalculator.calculateMA(dailyList, 5);
        BigDecimal[] ma20 = indicatorCalculator.calculateMA(dailyList, 20);
        IndicatorCalculator.MACDResult macd = indicatorCalculator.calculateMACD(dailyList);
        IndicatorCalculator.KDJResult kdj = indicatorCalculator.calculateKDJ(dailyList, 9, 3, 3);

        boolean holding = false;
        BigDecimal buyPrice = BigDecimal.ZERO;
        int shares = 0;

        for (int i = 1; i < dailyList.size(); i++) {
            StockDaily daily = dailyList.get(i);
            boolean buySignal = false;
            boolean sellSignal = false;

            // 根据策略类型生成信号
            switch (strategyType) {
                case "MA_GOLDEN_CROSS":
                    // 均线金叉策略：MA5上穿MA20买入，下穿卖出
                    if (ma5[i] != null && ma20[i] != null &&
                        ma5[i-1] != null && ma20[i-1] != null) {
                        buySignal = ma5[i-1].compareTo(ma20[i-1]) <= 0 &&
                                   ma5[i].compareTo(ma20[i]) > 0;
                        sellSignal = ma5[i-1].compareTo(ma20[i-1]) >= 0 &&
                                    ma5[i].compareTo(ma20[i]) < 0;
                    }
                    break;

                case "MACD_CROSS":
                    // MACD交叉策略：DIF上穿DEA买入
                    if (macd.getDif()[i] != null && macd.getDea()[i] != null &&
                        macd.getDif()[i-1] != null && macd.getDea()[i-1] != null) {
                        buySignal = macd.getDif()[i-1].compareTo(macd.getDea()[i-1]) <= 0 &&
                                   macd.getDif()[i].compareTo(macd.getDea()[i]) > 0;
                        sellSignal = macd.getDif()[i-1].compareTo(macd.getDea()[i-1]) >= 0 &&
                                    macd.getDif()[i].compareTo(macd.getDea()[i]) < 0;
                    }
                    break;

                case "KDJ_OVERSOLD":
                    // KDJ超卖策略：J值<20买入，>80卖出
                    if (kdj.getJ()[i] != null) {
                        buySignal = kdj.getJ()[i].compareTo(BigDecimal.valueOf(20)) < 0;
                        sellSignal = kdj.getJ()[i].compareTo(BigDecimal.valueOf(80)) > 0;
                    }
                    break;

                default:
                    // 默认使用均线金叉策略
                    if (ma5[i] != null && ma20[i] != null &&
                        ma5[i-1] != null && ma20[i-1] != null) {
                        buySignal = ma5[i-1].compareTo(ma20[i-1]) <= 0 &&
                                   ma5[i].compareTo(ma20[i]) > 0;
                        sellSignal = ma5[i-1].compareTo(ma20[i-1]) >= 0 &&
                                    ma5[i].compareTo(ma20[i]) < 0;
                    }
            }

            // 执行买入
            if (buySignal && !holding) {
                buyPrice = daily.getClose();
                shares = 1000; // 假设每次买入1000股
                holding = true;
                trades.add(new TradeRecord(daily.getTradeDate(), "BUY", buyPrice, shares));
            }

            // 执行卖出
            if (sellSignal && holding) {
                BigDecimal sellPrice = daily.getClose();
                trades.add(new TradeRecord(daily.getTradeDate(), "SELL", sellPrice, shares));
                holding = false;
                shares = 0;
            }
        }

        return trades;
    }

    /**
     * 计算收益指标
     */
    private void calculatePerformanceMetrics(BacktestResult result,
                                              List<StockDaily> dailyList,
                                              List<TradeRecord> trades,
                                              BigDecimal initialCapital) {
        // 计算总收益
        BigDecimal totalProfit = BigDecimal.ZERO;
        int winTrades = 0;
        int lossTrades = 0;
        BigDecimal totalWin = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;

        for (int i = 0; i < trades.size() - 1; i += 2) {
            if (i + 1 < trades.size()) {
                TradeRecord buy = trades.get(i);
                TradeRecord sell = trades.get(i + 1);
                if ("BUY".equals(buy.type) && "SELL".equals(sell.type)) {
                    BigDecimal profit = sell.price.subtract(buy.price)
                        .multiply(BigDecimal.valueOf(buy.shares));
                    totalProfit = totalProfit.add(profit);

                    if (profit.compareTo(BigDecimal.ZERO) > 0) {
                        winTrades++;
                        totalWin = totalWin.add(profit);
                    } else {
                        lossTrades++;
                        totalLoss = totalLoss.add(profit.abs());
                    }
                }
            }
        }

        BigDecimal finalCapital = initialCapital.add(totalProfit);
        result.setFinalCapital(finalCapital);

        // 总收益率
        BigDecimal totalReturn = finalCapital.subtract(initialCapital)
            .divide(initialCapital, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        result.setTotalReturn(totalReturn);

        // 年化收益率
        int days = dailyList.size();
        BigDecimal annualReturn = BigDecimal.ZERO;
        if (days > 0 && totalReturn.compareTo(BigDecimal.ZERO) != 0) {
            double years = days / (double) TRADING_DAYS_PER_YEAR;
            double returnRate = totalReturn.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).doubleValue();
            annualReturn = BigDecimal.valueOf((Math.pow(1 + returnRate, 1.0 / years) - 1) * 100)
                .setScale(4, RoundingMode.HALF_UP);
        }
        result.setAnnualReturn(annualReturn);

        // 最大回撤
        BigDecimal maxDrawdown = calculateMaxDrawdown(dailyList);
        result.setMaxDrawdown(maxDrawdown);

        // 夏普比率
        BigDecimal sharpeRatio = calculateSharpeRatio(dailyList);
        result.setSharpeRatio(sharpeRatio);

        // 交易统计
        result.setTotalTrades(trades.size() / 2);
        result.setWinTrades(winTrades);
        result.setLossTrades(lossTrades);

        // 胜率
        if (result.getTotalTrades() > 0) {
            BigDecimal winRate = BigDecimal.valueOf(winTrades)
                .divide(BigDecimal.valueOf(result.getTotalTrades()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            result.setWinRate(winRate);
        }

        // 平均盈亏
        if (winTrades > 0) {
            result.setAvgProfit(totalWin.divide(BigDecimal.valueOf(winTrades), 2, RoundingMode.HALF_UP));
        }
        if (lossTrades > 0) {
            result.setAvgLoss(totalLoss.divide(BigDecimal.valueOf(lossTrades), 2, RoundingMode.HALF_UP));
        }

        // 盈亏比
        if (result.getAvgLoss() != null && result.getAvgLoss().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal plr = result.getAvgProfit().divide(result.getAvgLoss(), 4, RoundingMode.HALF_UP);
            result.setProfitLossRatio(plr);
        }
    }

    /**
     * 计算最大回撤
     */
    private BigDecimal calculateMaxDrawdown(List<StockDaily> dailyList) {
        BigDecimal maxPrice = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (StockDaily daily : dailyList) {
            if (daily.getClose().compareTo(maxPrice) > 0) {
                maxPrice = daily.getClose();
            }

            BigDecimal drawdown = maxPrice.subtract(daily.getClose())
                .divide(maxPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }

    /**
     * 计算夏普比率
     */
    private BigDecimal calculateSharpeRatio(List<StockDaily> dailyList) {
        if (dailyList.size() < 2) {
            return BigDecimal.ZERO;
        }

        // 计算日收益率
        List<BigDecimal> dailyReturns = new ArrayList<>();
        for (int i = 1; i < dailyList.size(); i++) {
            BigDecimal ret = dailyList.get(i).getClose()
                .subtract(dailyList.get(i-1).getClose())
                .divide(dailyList.get(i-1).getClose(), 6, RoundingMode.HALF_UP);
            dailyReturns.add(ret);
        }

        // 计算平均收益率
        BigDecimal avgReturn = dailyReturns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(dailyReturns.size()), 6, RoundingMode.HALF_UP);

        // 计算收益率标准差
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal ret : dailyReturns) {
            variance = variance.add(ret.subtract(avgReturn).pow(2));
        }
        variance = variance.divide(BigDecimal.valueOf(dailyReturns.size()), 6, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // 夏普比率 = (年化收益 - 无风险收益) / 年化标准差
        if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal annualizedReturn = avgReturn.multiply(BigDecimal.valueOf(TRADING_DAYS_PER_YEAR));
        BigDecimal annualizedStdDev = stdDev.multiply(BigDecimal.valueOf(Math.sqrt(TRADING_DAYS_PER_YEAR)));

        return annualizedReturn.subtract(RISK_FREE_RATE)
            .divide(annualizedStdDev, 4, RoundingMode.HALF_UP);
    }

    /**
     * 转换交易记录为JSON字符串
     */
    private String convertTradesToJson(List<TradeRecord> trades) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < trades.size(); i++) {
            TradeRecord t = trades.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"date\":\"%s\",\"type\":\"%s\",\"price\":%s,\"shares\":%d}",
                t.date, t.type, t.price, t.shares));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 查询回测结果
     */
    public List<BacktestResult> getBacktestResults(String stockCode) {
        return backtestResultRepository.findByStockCode(stockCode);
    }

    /**
     * 获取最新的回测结果
     */
    public BacktestResult getLatestBacktestResult(Long strategyId) {
        return backtestResultRepository.findLatestByStrategyId(strategyId);
    }

    // ========== 内部类：交易记录 ==========

    /**
     * 交易记录
     */
    private static class TradeRecord {
        LocalDate date;
        String type;
        BigDecimal price;
        int shares;

        TradeRecord(LocalDate date, String type, BigDecimal price, int shares) {
            this.date = date;
            this.type = type;
            this.price = price;
            this.shares = shares;
        }
    }
}