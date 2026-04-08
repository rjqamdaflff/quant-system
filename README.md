# Quant System - 股票量化交易系统

基于 Java Spring Boot + Python 的 A 股量化分析平台，提供数据采集、策略研究、回测验证、信号预测和交易提醒功能。

## 功能特性

### ✅ 模块1：数据采集层
- 真实数据采集（AKShare/Baostock 数据源）
- 禁止模拟数据，网络不可用时抛出异常
- 数据验证和清洗
- 批量采集和增量采集
- 重试机制
- 采集日志记录

### ✅ 模块2：数据存储层
- 完善的数据表结构（股票信息、日线、分钟线、财务数据）
- 数据库索引优化
- 策略配置存储
- 回测结果存储
- 信号提醒存储

### ✅ 模块3：数据清洗层
- 停牌数据检测和标记
- 缺失值填充（前值填充、线性插值）
- 异常值检测和处理
- 复权计算（前复权）
- 数据完整性检查

### ✅ 模块4：策略分析层
- 技术指标计算（MA、MACD、KDJ、RSI、BOLL）
- 策略回测引擎
- 收益评估（年化收益、最大回撤、夏普比率）
- 内置策略（均线金叉、MACD交叉、KDJ超卖、布林带）

### ✅ 模块5：预测提醒层
- 信号生成服务
- 买卖点识别
- 风险预警
- 多渠道推送（邮件、钉钉、飞书、企业微信）

### ✅ 前端界面
- Vue.js 3 + TypeScript
- Element Plus UI框架
- ECharts 图表库
- 首页仪表盘、数据管理、策略研究、回测分析、信号提醒

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 后端开发 |
| Spring Boot | 3.2.0 | 框架 |
| MySQL/MariaDB | 10.11 | 数据存储 |
| Python | 3.x | 数据采集脚本 |
| AKShare | 1.18.x | A股数据源 |
| Baostock | 0.8.x | 备用数据源 |
| Vue.js | 3.4 | 前端框架 |
| Element Plus | 2.4 | UI组件库 |
| ECharts | 5.4 | 图表库 |

## 项目结构

```
quant-system/
├── scripts/
│   └── data_collector.py          # Python 数据采集脚本
├── src/main/java/com/quant/
│   ├── controller/                 # REST API 控制器
│   │   ├── DataCollectController.java
│   │   ├── StrategyController.java
│   │   └── SignalController.java
│   ├── entity/                     # JPA 实体类
│   │   ├── StockInfo.java
│   │   ├── StockDaily.java
│   │   ├── StockMinute.java
│   │   ├── StockFinancial.java
│   │   ├── StrategyConfig.java
│   │   ├── BacktestResult.java
│   │   └── SignalAlert.java
│   ├── repository/                 # 数据仓库
│   └── service/                    # 业务服务
│       ├── StockDataCollectorService.java
│       ├── DataValidationService.java
│       ├── DataCleaningService.java
│       ├── IndicatorCalculator.java
│       ├── BacktestService.java
│       ├── SignalGenerator.java
│       └── AlertService.java
├── src/test/                       # 测试代码
├── frontend/                       # Vue.js 前端
│   ├── src/
│   │   ├── views/                  # 页面组件
│   │   ├── api/                    # API接口
│   │   └── router/                 # 路由配置
│   └── package.json
├── docs/                           # 文档
│   ├── architecture.md             # 架构设计
│   ├── api.md                      # API接口文档
│   ├── deployment.md               # 部署指南
│   └── strategy.md                 # 策略开发指南
└── pom.xml
```

## 快速开始

### 1. 环境准备

```bash
# 安装Python依赖
pip install akshare baostock

# 安装Node.js依赖（前端）
cd frontend && npm install
```

### 2. 配置数据库

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quant_db
    username: root
    password: your_password
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm run dev
```

访问 http://localhost:3000

## API 接口

### 数据采集 API

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/collect/stocks | GET | 获取股票列表 |
| /api/collect/status | GET | 系统状态 |
| /api/collect/daily/{code} | GET | 查询日线数据 |
| /api/collect/daily/{code} | POST | 采集单只股票 |
| /api/collect/daily/batch | POST | 批量采集 |
| /api/collect/validate/{code} | GET | 验证数据 |

### 策略分析 API

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/strategy/types | GET | 策略类型列表 |
| /api/strategy/list | GET | 策略配置列表 |
| /api/strategy/config | POST | 创建策略配置 |
| /api/strategy/backtest | POST | 执行回测 |

### 信号提醒 API

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/signal/list | GET | 信号列表 |
| /api/signal/generate | POST | 生成信号 |
| /api/signal/send/{id} | POST | 发送信号 |
| /api/signal/send-all | POST | 批量发送 |

## 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=StockDataCollectorServiceTest
```

## 文档

- [架构设计文档](docs/architecture.md)
- [API接口文档](docs/api.md)
- [部署指南](docs/deployment.md)
- [策略开发指南](docs/strategy.md)

## 内置策略

| 策略 | 说明 |
|------|------|
| MA_GOLDEN_CROSS | 均线金叉策略：MA5上穿MA20买入 |
| MACD_CROSS | MACD交叉策略：DIF上穿DEA买入 |
| KDJ_OVERSOLD | KDJ超卖策略：J值<20买入 |
| BOLL_BAND | 布林带策略：触及下轨买入 |

## 数据验证规则

- 涨跌幅范围：-11% ~ +11%（含ST股）
- 价格逻辑：最高价 >= 最低价
- 成交量：不能为负数
- 价格：必须为正数

## 参考项目

部分设计参考了 [daily_stock_analysis](https://github.com/ZhuLinsen/daily_stock_analysis) 项目的优秀实践。

## License

MIT