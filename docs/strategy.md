# 策略开发指南

## 1. 概述

本文档介绍如何在 Quant System 中开发和自定义交易策略。

## 2. 策略接口定义

所有策略实现 `Strategy` 接口：

```java
public interface Strategy {
    /**
     * 获取策略名称
     */
    String getName();
    
    /**
     * 获取策略参数定义
     */
    Map<String, Object> getParameters();
    
    /**
     * 生成交易信号
     * @param dailyList 日线数据列表
     * @param params 策略参数
     * @return 信号列表
     */
    List<Signal> generateSignals(List<StockDaily> dailyList, Map<String, Object> params);
}
```

## 3. 内置策略

### 3.1 均线金叉策略 (MA_GOLDEN_CROSS)

**原理**：
- 买入信号：短期均线上穿长期均线（金叉）
- 卖出信号：短期均线下穿长期均线（死叉）

**参数**：
| 参数 | 说明 | 默认值 |
|------|------|--------|
| shortPeriod | 短期均线周期 | 5 |
| longPeriod | 长期均线周期 | 20 |

**代码示例**：

```java
public class GoldenCrossStrategy implements Strategy {
    
    @Override
    public String getName() {
        return "均线金叉策略";
    }
    
    @Override
    public List<Signal> generateSignals(List<StockDaily> dailyList, Map<String, Object> params) {
        int shortPeriod = (int) params.getOrDefault("shortPeriod", 5);
        int longPeriod = (int) params.getOrDefault("longPeriod", 20);
        
        BigDecimal[] maShort = indicatorCalculator.calculateMA(dailyList, shortPeriod);
        BigDecimal[] maLong = indicatorCalculator.calculateMA(dailyList, longPeriod);
        
        List<Signal> signals = new ArrayList<>();
        
        for (int i = 1; i < dailyList.size(); i++) {
            // 金叉：短期均线从下方穿过长期均线
            if (maShort[i-1].compareTo(maLong[i-1]) <= 0 &&
                maShort[i].compareTo(maLong[i]) > 0) {
                signals.add(new Signal("BUY", dailyList.get(i)));
            }
            // 死叉：短期均线从上方穿过长期均线
            if (maShort[i-1].compareTo(maLong[i-1]) >= 0 &&
                maShort[i].compareTo(maLong[i]) < 0) {
                signals.add(new Signal("SELL", dailyList.get(i)));
            }
        }
        
        return signals;
    }
}
```

### 3.2 MACD交叉策略 (MACD_CROSS)

**原理**：
- 买入信号：DIF线上穿DEA线
- 卖出信号：DIF线下穿DEA线

**参数**：
| 参数 | 说明 | 默认值 |
|------|------|--------|
| fastPeriod | 快线周期 | 12 |
| slowPeriod | 慢线周期 | 26 |
| signalPeriod | 信号线周期 | 9 |

### 3.3 KDJ超卖策略 (KDJ_OVERSOLD)

**原理**：
- 买入信号：J值低于超卖线（默认20）
- 卖出信号：J值高于超买线（默认80）

**参数**：
| 参数 | 说明 | 默认值 |
|------|------|--------|
| n | KDJ周期 | 9 |
| m1 | K值平滑周期 | 3 |
| m2 | D值平滑周期 | 3 |
| oversold | 超卖阈值 | 20 |
| overbought | 超买阈值 | 80 |

### 3.4 布林带策略 (BOLL_BAND)

**原理**：
- 买入信号：价格触及下轨
- 卖出信号：价格触及上轨

**参数**：
| 参数 | 说明 | 默认值 |
|------|------|--------|
| period | 周期 | 20 |
| stdDevTimes | 标准差倍数 | 2 |

## 4. 自定义策略开发

### 4.1 创建策略类

```java
@Service
public class MyCustomStrategy implements Strategy {
    
    private final IndicatorCalculator indicatorCalculator;
    
    public MyCustomStrategy(IndicatorCalculator indicatorCalculator) {
        this.indicatorCalculator = indicatorCalculator;
    }
    
    @Override
    public String getName() {
        return "我的自定义策略";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", 10);
        params.put("param2", 0.5);
        return params;
    }
    
    @Override
    public List<Signal> generateSignals(List<StockDaily> dailyList, Map<String, Object> params) {
        // 1. 计算技术指标
        BigDecimal[] ma20 = indicatorCalculator.calculateMA(dailyList, 20);
        IndicatorCalculator.KDJResult kdj = indicatorCalculator.calculateKDJ(dailyList, 9, 3, 3);
        
        // 2. 定义交易规则
        List<Signal> signals = new ArrayList<>();
        
        for (int i = 20; i < dailyList.size(); i++) {
            StockDaily daily = dailyList.get(i);
            
            // 示例规则：价格低于MA20且KDJ超卖
            if (daily.getClose().compareTo(ma20[i]) < 0 &&
                kdj.getJ()[i].compareTo(BigDecimal.valueOf(20)) < 0) {
                
                Signal signal = new Signal();
                signal.setType("BUY");
                signal.setStockCode(daily.getStockCode());
                signal.setTriggerPrice(daily.getClose());
                signal.setTriggerCondition("价格低于MA20且KDJ超卖");
                signals.add(signal);
            }
        }
        
        return signals;
    }
}
```

### 4.2 注册策略

在 `StrategyEngine` 中注册新策略：

```java
@Service
public class StrategyEngine {
    
    private final Map<String, Strategy> strategies = new HashMap<>();
    
    @PostConstruct
    public void init() {
        strategies.put("MA_GOLDEN_CROSS", new GoldenCrossStrategy());
        strategies.put("MACD_CROSS", new MACDCrossStrategy());
        strategies.put("KDJ_OVERSOLD", new KDJOversoldStrategy());
        strategies.put("BOLL_BAND", new BollingerBandStrategy());
        strategies.put("MY_CUSTOM", new MyCustomStrategy(indicatorCalculator));  // 注册自定义策略
    }
    
    public Strategy getStrategy(String strategyType) {
        return strategies.get(strategyType);
    }
}
```

## 5. 组合策略

可以将多个策略组合使用：

```java
public class CombinedStrategy implements Strategy {
    
    private final List<Strategy> subStrategies;
    
    public CombinedStrategy(List<Strategy> subStrategies) {
        this.subStrategies = subStrategies;
    }
    
    @Override
    public List<Signal> generateSignals(List<StockDaily> dailyList, Map<String, Object> params) {
        List<Signal> allSignals = new ArrayList<>();
        
        for (Strategy strategy : subStrategies) {
            List<Signal> signals = strategy.generateSignals(dailyList, params);
            allSignals.addAll(signals);
        }
        
        // 投票机制：多个策略同时发出相同信号时才触发
        return filterByVoting(allSignals);
    }
    
    private List<Signal> filterByVoting(List<Signal> signals) {
        // 实现投票逻辑
        // ...
    }
}
```

## 6. 策略优化

### 6.1 参数优化

使用网格搜索优化策略参数：

```java
public Map<String, Object> optimizeParameters(List<StockDaily> dailyList) {
    double bestReturn = Double.MIN_VALUE;
    Map<String, Object> bestParams = new HashMap<>();
    
    // 遍历参数组合
    for (int shortPeriod = 5; shortPeriod <= 20; shortPeriod += 5) {
        for (int longPeriod = 20; longPeriod <= 60; longPeriod += 10) {
            Map<String, Object> params = new HashMap<>();
            params.put("shortPeriod", shortPeriod);
            params.put("longPeriod", longPeriod);
            
            // 运行回测
            double returnValue = runBacktest(dailyList, params);
            
            if (returnValue > bestReturn) {
                bestReturn = returnValue;
                bestParams = params;
            }
        }
    }
    
    return bestParams;
}
```

### 6.2 止损止盈

为策略添加止损止盈逻辑：

```java
public class StopLossStrategy implements Strategy {
    
    private final Strategy baseStrategy;
    private final double stopLossPct;   // 止损比例
    private final double stopProfitPct; // 止盈比例
    
    @Override
    public List<Signal> generateSignals(List<StockDaily> dailyList, Map<String, Object> params) {
        List<Signal> signals = baseStrategy.generateSignals(dailyList, params);
        List<Signal> filteredSignals = new ArrayList<>();
        
        BigDecimal buyPrice = null;
        
        for (int i = 0; i < signals.size(); i++) {
            Signal signal = signals.get(i);
            StockDaily daily = dailyList.get(i);
            
            if ("BUY".equals(signal.getType())) {
                buyPrice = daily.getClose();
                filteredSignals.add(signal);
            } else if ("SELL".equals(signal.getType()) && buyPrice != null) {
                BigDecimal currentPrice = daily.getClose();
                double changePct = currentPrice.subtract(buyPrice)
                    .divide(buyPrice, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
                
                // 检查止损止盈
                if (changePct <= -stopLossPct) {
                    signal.setTriggerCondition("触发止损: " + changePct + "%");
                    filteredSignals.add(signal);
                    buyPrice = null;
                } else if (changePct >= stopProfitPct) {
                    signal.setTriggerCondition("触发止盈: " + changePct + "%");
                    filteredSignals.add(signal);
                    buyPrice = null;
                } else {
                    filteredSignals.add(signal);
                    buyPrice = null;
                }
            }
        }
        
        return filteredSignals;
    }
}
```

## 7. 回测验证

### 7.1 历史回测

```java
@SpringBootTest
public class StrategyTest {
    
    @Autowired
    private BacktestService backtestService;
    
    @Test
    public void testGoldenCrossStrategy() {
        BacktestResult result = backtestService.runBacktest(
            "600519",
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 12, 31),
            new BigDecimal("100000"),
            "MA_GOLDEN_CROSS"
        );
        
        System.out.println("总收益率: " + result.getTotalReturn() + "%");
        System.out.println("最大回撤: " + result.getMaxDrawdown() + "%");
        System.out.println("夏普比率: " + result.getSharpeRatio());
    }
}
```

### 7.2 参数敏感性分析

```java
@Test
public void parameterSensitivityAnalysis() {
    int[] shortPeriods = {5, 10, 15};
    int[] longPeriods = {20, 30, 60};
    
    for (int shortP : shortPeriods) {
        for (int longP : longPeriods) {
            // 设置参数并运行回测
            // 比较不同参数组合的结果
        }
    }
}
```

## 8. 最佳实践

1. **避免过度拟合**：使用样本外数据验证策略
2. **考虑交易成本**：在回测中加入手续费和滑点
3. **风险控制**：设置合理的止损止盈
4. **多策略分散**：不要依赖单一策略
5. **定期回顾**：策略有效性会随市场变化