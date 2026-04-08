package com.quant.service;

import com.quant.entity.SignalAlert;
import com.quant.entity.StockDaily;
import com.quant.repository.SignalAlertRepository;
import com.quant.repository.StockDailyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 信号生成服务
 *
 * 基于技术指标生成交易信号：
 * 1. 买入信号：金叉、超卖、突破等
 * 2. 卖出信号：死叉、超买、跌破支撑等
 * 3. 风险预警：跌破均线、量价异常等
 *
 * 所有信号基于真实数据计算，不生成虚假信号
 */
@Service
public class SignalGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SignalGenerator.class);

    private final SignalAlertRepository signalAlertRepository;
    private final StockDailyRepository stockDailyRepository;
    private final IndicatorCalculator indicatorCalculator;

    public SignalGenerator(SignalAlertRepository signalAlertRepository,
                           StockDailyRepository stockDailyRepository,
                           IndicatorCalculator indicatorCalculator) {
        this.signalAlertRepository = signalAlertRepository;
        this.stockDailyRepository = stockDailyRepository;
        this.indicatorCalculator = indicatorCalculator;
    }

    /**
     * 为指定股票生成交易信号
     *
     * @param stockCode 股票代码
     * @param stockName 股票名称
     * @param days 分析天数
     * @return 生成的信号列表
     */
    public List<SignalAlert> generateSignals(String stockCode, String stockName, int days) {
        logger.info("生成交易信号: 股票={}, 分析天数={}", stockCode, days);

        List<SignalAlert> signals = new ArrayList<>();

        // 获取历史数据
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days + 100); // 额外数据用于计算指标

        List<StockDaily> dailyList = stockDailyRepository
            .findByStockCodeAndTradeDateBetween(stockCode, startDate, endDate);

        if (dailyList.size() < 60) {
            logger.warn("股票 {} 数据不足，无法生成信号", stockCode);
            return signals;
        }

        // 计算技术指标
        BigDecimal[] ma5 = indicatorCalculator.calculateMA(dailyList, 5);
        BigDecimal[] ma10 = indicatorCalculator.calculateMA(dailyList, 10);
        BigDecimal[] ma20 = indicatorCalculator.calculateMA(dailyList, 20);
        BigDecimal[] ma60 = indicatorCalculator.calculateMA(dailyList, 60);
        IndicatorCalculator.MACDResult macd = indicatorCalculator.calculateMACD(dailyList);
        IndicatorCalculator.KDJResult kdj = indicatorCalculator.calculateKDJ(dailyList, 9, 3, 3);
        IndicatorCalculator.BOLLResult boll = indicatorCalculator.calculateBOLL(dailyList, 20, 2);

        int lastIndex = dailyList.size() - 1;
        StockDaily latestDaily = dailyList.get(lastIndex);

        // 1. 检测均线金叉信号
        SignalAlert maSignal = checkMACrossSignal(stockCode, stockName, dailyList, ma5, ma10, ma20);
        if (maSignal != null) {
            signals.add(maSignal);
        }

        // 2. 检测MACD信号
        SignalAlert macdSignal = checkMACDSignal(stockCode, stockName, dailyList, macd);
        if (macdSignal != null) {
            signals.add(macdSignal);
        }

        // 3. 检测KDJ信号
        SignalAlert kdjSignal = checkKDJSignal(stockCode, stockName, dailyList, kdj);
        if (kdjSignal != null) {
            signals.add(kdjSignal);
        }

        // 4. 检测布林带信号
        SignalAlert bollSignal = checkBOLLSignal(stockCode, stockName, dailyList, boll);
        if (bollSignal != null) {
            signals.add(bollSignal);
        }

        // 5. 检测风险预警
        SignalAlert riskSignal = checkRiskWarning(stockCode, stockName, dailyList, ma20, ma60);
        if (riskSignal != null) {
            signals.add(riskSignal);
        }

        // 保存信号
        for (SignalAlert signal : signals) {
            signal.setCreateTime(LocalDateTime.now());
            signal.setAlertStatus("PENDING");
        }
        signalAlertRepository.saveAll(signals);

        logger.info("生成信号数量: {}", signals.size());
        return signals;
    }

    /**
     * 检测均线交叉信号
     */
    private SignalAlert checkMACrossSignal(String stockCode, String stockName,
                                            List<StockDaily> dailyList,
                                            BigDecimal[] ma5, BigDecimal[] ma10, BigDecimal[] ma20) {
        int last = dailyList.size() - 1;
        int prev = last - 1;

        if (ma5[last] == null || ma20[last] == null ||
            ma5[prev] == null || ma20[prev] == null) {
            return null;
        }

        SignalAlert signal = new SignalAlert();
        signal.setStockCode(stockCode);
        signal.setStockName(stockName);
        signal.setSignalTime(LocalDateTime.now());
        signal.setTriggerPrice(dailyList.get(last).getClose());

        // 金叉：MA5上穿MA20
        if (ma5[prev].compareTo(ma20[prev]) <= 0 && ma5[last].compareTo(ma20[last]) > 0) {
            signal.setSignalType("BUY");
            signal.setTriggerCondition("MA5上穿MA20形成金叉");
            signal.setSignalStrength("MEDIUM");
            signal.setSuggestion("BUY");
            return signal;
        }

        // 死叉：MA5下穿MA20
        if (ma5[prev].compareTo(ma20[prev]) >= 0 && ma5[last].compareTo(ma20[last]) < 0) {
            signal.setSignalType("SELL");
            signal.setTriggerCondition("MA5下穿MA20形成死叉");
            signal.setSignalStrength("MEDIUM");
            signal.setSuggestion("SELL");
            return signal;
        }

        return null;
    }

    /**
     * 检测MACD信号
     */
    private SignalAlert checkMACDSignal(String stockCode, String stockName,
                                         List<StockDaily> dailyList,
                                         IndicatorCalculator.MACDResult macd) {
        int last = dailyList.size() - 1;
        int prev = last - 1;

        if (macd.getDif()[last] == null || macd.getDea()[last] == null ||
            macd.getDif()[prev] == null || macd.getDea()[prev] == null) {
            return null;
        }

        SignalAlert signal = new SignalAlert();
        signal.setStockCode(stockCode);
        signal.setStockName(stockName);
        signal.setSignalTime(LocalDateTime.now());
        signal.setTriggerPrice(dailyList.get(last).getClose());

        // DIF上穿DEA
        if (macd.getDif()[prev].compareTo(macd.getDea()[prev]) <= 0 &&
            macd.getDif()[last].compareTo(macd.getDea()[last]) > 0) {
            signal.setSignalType("BUY");
            signal.setTriggerCondition("MACD金叉：DIF上穿DEA");
            signal.setSignalStrength("STRONG");
            signal.setSuggestion("BUY");
            return signal;
        }

        // DIF下穿DEA
        if (macd.getDif()[prev].compareTo(macd.getDea()[prev]) >= 0 &&
            macd.getDif()[last].compareTo(macd.getDea()[last]) < 0) {
            signal.setSignalType("SELL");
            signal.setTriggerCondition("MACD死叉：DIF下穿DEA");
            signal.setSignalStrength("STRONG");
            signal.setSuggestion("SELL");
            return signal;
        }

        return null;
    }

    /**
     * 检测KDJ信号
     */
    private SignalAlert checkKDJSignal(String stockCode, String stockName,
                                        List<StockDaily> dailyList,
                                        IndicatorCalculator.KDJResult kdj) {
        int last = dailyList.size() - 1;

        if (kdj.getJ()[last] == null) {
            return null;
        }

        BigDecimal jValue = kdj.getJ()[last];

        // 超卖区域
        if (jValue.compareTo(BigDecimal.valueOf(20)) < 0) {
            SignalAlert signal = new SignalAlert();
            signal.setStockCode(stockCode);
            signal.setStockName(stockName);
            signal.setSignalType("BUY");
            signal.setSignalTime(LocalDateTime.now());
            signal.setTriggerPrice(dailyList.get(last).getClose());
            signal.setTriggerCondition(String.format("KDJ超卖：J值=%.2f", jValue));
            signal.setSignalStrength(jValue.compareTo(BigDecimal.ZERO) < 0 ? "STRONG" : "MEDIUM");
            signal.setSuggestion("BUY");
            return signal;
        }

        // 超买区域
        if (jValue.compareTo(BigDecimal.valueOf(80)) > 0) {
            SignalAlert signal = new SignalAlert();
            signal.setStockCode(stockCode);
            signal.setStockName(stockName);
            signal.setSignalType("SELL");
            signal.setSignalTime(LocalDateTime.now());
            signal.setTriggerPrice(dailyList.get(last).getClose());
            signal.setTriggerCondition(String.format("KDJ超买：J值=%.2f", jValue));
            signal.setSignalStrength(jValue.compareTo(BigDecimal.valueOf(100)) > 0 ? "STRONG" : "MEDIUM");
            signal.setSuggestion("SELL");
            return signal;
        }

        return null;
    }

    /**
     * 检测布林带信号
     */
    private SignalAlert checkBOLLSignal(String stockCode, String stockName,
                                         List<StockDaily> dailyList,
                                         IndicatorCalculator.BOLLResult boll) {
        int last = dailyList.size() - 1;

        if (boll.getUpper()[last] == null || boll.getLower()[last] == null) {
            return null;
        }

        BigDecimal closePrice = dailyList.get(last).getClose();
        BigDecimal upper = boll.getUpper()[last];
        BigDecimal lower = boll.getLower()[last];

        SignalAlert signal = new SignalAlert();
        signal.setStockCode(stockCode);
        signal.setStockName(stockName);
        signal.setSignalTime(LocalDateTime.now());
        signal.setTriggerPrice(closePrice);

        // 触及下轨
        if (closePrice.compareTo(lower) <= 0) {
            signal.setSignalType("BUY");
            signal.setTriggerCondition("价格触及布林带下轨，可能超卖");
            signal.setSignalStrength("MEDIUM");
            signal.setSuggestion("BUY");
            return signal;
        }

        // 触及上轨
        if (closePrice.compareTo(upper) >= 0) {
            signal.setSignalType("SELL");
            signal.setTriggerCondition("价格触及布林带上轨，可能超买");
            signal.setSignalStrength("MEDIUM");
            signal.setSuggestion("SELL");
            return signal;
        }

        return null;
    }

    /**
     * 检测风险预警
     */
    private SignalAlert checkRiskWarning(String stockCode, String stockName,
                                          List<StockDaily> dailyList,
                                          BigDecimal[] ma20, BigDecimal[] ma60) {
        int last = dailyList.size() - 1;

        if (ma20[last] == null || ma60[last] == null) {
            return null;
        }

        BigDecimal closePrice = dailyList.get(last).getClose();

        // 跌破20日均线
        if (closePrice.compareTo(ma20[last]) < 0) {
            SignalAlert signal = new SignalAlert();
            signal.setStockCode(stockCode);
            signal.setStockName(stockName);
            signal.setSignalType("RISK_WARNING");
            signal.setSignalTime(LocalDateTime.now());
            signal.setTriggerPrice(closePrice);
            signal.setTriggerCondition("价格跌破20日均线，注意风险");
            signal.setSignalStrength("WEAK");
            signal.setSuggestion("WATCH");
            return signal;
        }

        // 跌破60日均线（更严重）
        if (closePrice.compareTo(ma60[last]) < 0) {
            SignalAlert signal = new SignalAlert();
            signal.setStockCode(stockCode);
            signal.setStockName(stockName);
            signal.setSignalType("RISK_WARNING");
            signal.setSignalTime(LocalDateTime.now());
            signal.setTriggerPrice(closePrice);
            signal.setTriggerCondition("价格跌破60日均线，风险较高");
            signal.setSignalStrength("STRONG");
            signal.setSuggestion("SELL");
            return signal;
        }

        return null;
    }

    /**
     * 批量生成信号
     */
    public List<SignalAlert> batchGenerateSignals(List<String> stockCodes, int days) {
        List<SignalAlert> allSignals = new ArrayList<>();

        for (String stockCode : stockCodes) {
            try {
                List<SignalAlert> signals = generateSignals(stockCode, stockCode, days);
                allSignals.addAll(signals);
            } catch (Exception e) {
                logger.error("生成股票 {} 信号失败: {}", stockCode, e.getMessage());
            }
        }

        return allSignals;
    }

    /**
     * 获取待发送的信号
     */
    public List<SignalAlert> getPendingSignals() {
        return signalAlertRepository.findPendingSignals();
    }

    /**
     * 获取最近的信号
     */
    public List<SignalAlert> getRecentSignals(String stockCode, int limit) {
        return signalAlertRepository.findRecentByStockCode(stockCode, limit);
    }
}