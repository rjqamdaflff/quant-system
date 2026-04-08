# 系统架构设计

## 1. 系统概述

Quant System 是一个基于 Java Spring Boot + Python 的 A 股量化分析平台，提供数据采集、策略研究、回测验证、信号预测和交易提醒功能。

## 2. 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端界面 (Vue.js 3)                       │
│  ├─ 首页仪表盘  ├─ 数据管理页  ├─ 策略研究页  ├─ 信号提醒页      │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        后端服务 (Spring Boot 3.2)                │
├─────────────────────────────────────────────────────────────────┤
│ 模块1: 数据采集层    │ Python脚本调用AKShare/Baostock获取真实数据 │
│ 模块2: 数据存储层    │ MySQL存储，支持增量更新                    │
│ 模块3: 数据清洗层    │ 异常数据处理、缺失值填充、标准化            │
│ 模块4: 策略分析层    │ 技术指标计算、策略回测引擎                  │
│ 模块5: 预测提醒层    │ 信号预测、多渠道推送                        │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据层                                    │
│  ├─ MySQL (股票数据、用户配置)                                   │
│  ├─ Python Scripts (数据采集)                                    │
│  └─ AKShare/Baostock (真实数据源)                               │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 模块设计

### 3.1 数据采集层

**核心组件**：
- `StockDataCollectorService` - 核心采集服务
- `DataValidationService` - 数据验证服务
- `scripts/data_collector.py` - Python 数据采集脚本

**数据源**：
- AKShare：主要数据源，支持A股全面数据
- Baostock：备用数据源，提供历史数据补充

**采集策略**：
- 增量采集：只采集新增数据
- 重试机制：失败自动重试3次
- 数据验证：采集后自动验证数据有效性

### 3.2 数据存储层

**数据表设计**：

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| stock_info | 股票基本信息 | stock_code, stock_name, industry, list_date |
| stock_daily | 日线数据 | stock_code, trade_date, open, close, high, low, volume |
| stock_minute | 分钟线数据 | stock_code, minute_time, period_type, open, close |
| stock_financial | 财务数据 | stock_code, report_date, total_revenue, net_profit, eps |
| strategy_config | 策略配置 | strategy_name, strategy_type, strategy_params |
| backtest_result | 回测结果 | total_return, annual_return, max_drawdown, sharpe_ratio |
| signal_alert | 信号提醒 | stock_code, signal_type, trigger_condition, alert_status |

### 3.3 数据清洗层

**清洗流程**：

```
原始数据 → 停牌检测 → 缺失值处理 → 异常值检测 → 复权计算 → 数据入库
```

**清洗规则**：
1. **停牌处理**：成交量=0标记为停牌，不删除
2. **缺失填充**：前值填充优先，线性插值备选
3. **异常检测**：涨跌幅>11%标记为异常
4. **复权处理**：前复权计算，调整历史价格

### 3.4 策略分析层

**技术指标计算**：

| 指标 | 参数 | 计算方式 |
|------|------|----------|
| MA | 5/10/20/60日 | 简单移动平均 |
| MACD | 12/26/9 | EMA差值 |
| KDJ | 9/3/3 | 随机指标 |
| RSI | 6/12/24 | 相对强弱 |
| BOLL | 20日/2倍标准差 | 布林带 |

**内置策略**：

1. **均线金叉策略**：MA5上穿MA20买入，下穿卖出
2. **MACD交叉策略**：DIF上穿DEA买入，下穿卖出
3. **KDJ超卖策略**：J值<20买入，>80卖出
4. **布林带策略**：触及下轨买入，触及上轨卖出

**收益评估指标**：
- 总收益率
- 年化收益率
- 最大回撤
- 夏普比率
- 胜率
- 盈亏比

### 3.5 预测提醒层

**信号类型**：
- BUY：买入信号
- SELL：卖出信号
- RISK_WARNING：风险预警

**提醒渠道**：
- 邮件通知
- 钉钉机器人
- 飞书机器人
- 企业微信

## 4. 前端架构

**技术栈**：
- Vue.js 3 + TypeScript
- Element Plus UI框架
- ECharts 图表库
- Axios HTTP客户端

**页面结构**：

```
frontend/src/
├── views/           # 页面组件
│   ├── Dashboard.vue      # 首页仪表盘
│   ├── DataManage.vue     # 数据管理
│   ├── StrategyResearch.vue # 策略研究
│   ├── BacktestAnalysis.vue # 回测分析
│   └── SignalAlert.vue    # 信号提醒
├── components/      # 公共组件
├── api/             # API接口
└── router/          # 路由配置
```

## 5. 数据流

### 5.1 数据采集流程

```
用户请求 → Controller → StockDataCollectorService → Python脚本 → AKShare/Baostock
                                                                          ↓
                            数据验证 ← DataValidationService ← JSON数据 ←┘
                                ↓
                           数据入库 → StockDailyRepository → MySQL
```

### 5.2 回测执行流程

```
用户配置 → BacktestController → BacktestService
                                      ↓
                              获取历史数据 ← StockDailyRepository
                                      ↓
                              计算技术指标 → IndicatorCalculator
                                      ↓
                              执行策略逻辑 → 生成交易记录
                                      ↓
                              计算收益指标 → 评估报告
                                      ↓
                              保存结果 → BacktestResultRepository
```

## 6. 安全设计

- 数据源验证：禁止使用模拟数据
- 输入校验：所有用户输入进行校验
- 异常处理：网络异常抛出明确错误
- SQL注入防护：使用JPA参数化查询

## 7. 扩展性设计

- 策略扩展：实现新策略只需继承策略接口
- 数据源扩展：支持添加新的数据源适配器
- 通知渠道扩展：实现NotificationChannel接口
- 前端组件化：便于复用和扩展