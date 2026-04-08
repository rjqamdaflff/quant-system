# API 接口文档

## 基础信息

- 基础URL: `http://localhost:8080/api`
- 数据格式: JSON
- 编码: UTF-8

## 1. 数据采集 API

### 1.1 获取股票列表

**请求**:
```
GET /api/collect/stocks
```

**响应**:
```json
[
  {
    "stockCode": "600519",
    "stockName": "贵州茅台",
    "industry": "白酒",
    "listDate": "2001-08-27"
  }
]
```

### 1.2 获取系统状态

**请求**:
```
GET /api/collect/status
```

**响应**:
```json
{
  "dbConnected": true,
  "dataSourceOk": true,
  "lastCollectTime": "2024-01-15 15:30:00",
  "dataIntegrity": "99.5%"
}
```

### 1.3 采集单只股票日线数据

**请求**:
```
POST /api/collect/daily/{stockCode}
```

**响应**:
```json
{
  "success": true,
  "message": "采集完成",
  "dataCount": 250
}
```

### 1.4 批量采集日线数据

**请求**:
```
POST /api/collect/daily/batch
Content-Type: application/json

["600519", "000001", "000002"]
```

**响应**:
```json
{
  "success": true,
  "totalCollected": 750
}
```

### 1.5 查询日线数据

**请求**:
```
GET /api/collect/daily/{stockCode}?startDate=2024-01-01&endDate=2024-12-31
```

**响应**:
```json
[
  {
    "tradeDate": "2024-01-02",
    "open": 1850.00,
    "close": 1860.00,
    "high": 1870.00,
    "low": 1845.00,
    "volume": 2500000,
    "amount": 4650000000,
    "changePct": 0.54
  }
]
```

### 1.6 验证数据

**请求**:
```
GET /api/collect/validate/{stockCode}
```

**响应**:
```json
{
  "valid": true,
  "totalRecords": 250,
  "invalidRecords": 0,
  "issues": []
}
```

## 2. 策略分析 API

### 2.1 获取策略类型列表

**请求**:
```
GET /api/strategy/types
```

**响应**:
```json
[
  {
    "code": "MA_GOLDEN_CROSS",
    "name": "均线金叉策略",
    "desc": "MA5上穿MA20买入，下穿卖出"
  },
  {
    "code": "MACD_CROSS",
    "name": "MACD交叉策略",
    "desc": "DIF上穿DEA买入，下穿卖出"
  }
]
```

### 2.2 获取策略配置列表

**请求**:
```
GET /api/strategy/list
```

**响应**:
```json
[
  {
    "id": 1,
    "strategyName": "茅台均线策略",
    "strategyType": "MA_GOLDEN_CROSS",
    "targetStocks": "600519",
    "status": "ACTIVE",
    "createTime": "2024-01-15 10:00:00"
  }
]
```

### 2.3 创建策略配置

**请求**:
```
POST /api/strategy/config
Content-Type: application/json

{
  "strategyName": "茅台均线策略",
  "strategyType": "MA_GOLDEN_CROSS",
  "targetStocks": "600519",
  "stopLossPct": 5,
  "stopProfitPct": 10,
  "alertEnabled": true,
  "alertChannels": "EMAIL,DINGTALK"
}
```

**响应**:
```json
{
  "id": 1,
  "strategyName": "茅台均线策略",
  "status": "ACTIVE"
}
```

### 2.4 更新策略配置

**请求**:
```
PUT /api/strategy/config/{id}
Content-Type: application/json

{
  "strategyName": "更新后的策略名",
  "stopLossPct": 8
}
```

### 2.5 删除策略配置

**请求**:
```
DELETE /api/strategy/config/{id}
```

### 2.6 执行回测

**请求**:
```
POST /api/strategy/backtest
Content-Type: application/json

{
  "stockCode": "600519",
  "startDate": "2023-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 100000,
  "strategyType": "MA_GOLDEN_CROSS"
}
```

**响应**:
```json
{
  "id": 1,
  "stockCode": "600519",
  "totalReturn": 25.50,
  "annualReturn": 25.50,
  "maxDrawdown": 8.30,
  "sharpeRatio": 1.85,
  "totalTrades": 15,
  "winTrades": 10,
  "lossTrades": 5,
  "winRate": 66.67,
  "avgProfit": 3500.00,
  "avgLoss": 1800.00,
  "profitLossRatio": 1.94,
  "tradeRecords": "[{\"date\":\"2023-02-15\",\"type\":\"BUY\",\"price\":1800.00,\"shares\":1000}]",
  "backtestStatus": "COMPLETED",
  "backtestTime": "2024-01-15 15:00:00"
}
```

### 2.7 获取回测结果

**请求**:
```
GET /api/strategy/backtest/{stockCode}
```

## 3. 信号提醒 API

### 3.1 获取信号列表

**请求**:
```
GET /api/signal/list?stockCode=600519&status=PENDING
```

**响应**:
```json
[
  {
    "id": 1,
    "stockCode": "600519",
    "stockName": "贵州茅台",
    "signalType": "BUY",
    "triggerPrice": 1850.00,
    "triggerCondition": "MA5上穿MA20形成金叉",
    "signalStrength": "MEDIUM",
    "suggestion": "BUY",
    "alertStatus": "PENDING",
    "signalTime": "2024-01-15 14:30:00"
  }
]
```

### 3.2 生成信号

**请求**:
```
POST /api/signal/generate?stockCode=600519&stockName=贵州茅台&days=60
```

### 3.3 获取待发送信号

**请求**:
```
GET /api/signal/pending
```

### 3.4 发送单个信号

**请求**:
```
POST /api/signal/send/{id}?channels=EMAIL,DINGTALK
```

### 3.5 批量发送信号

**请求**:
```
POST /api/signal/send-all
```

**响应**:
```
"发送完成: 成功5, 失败0"
```

### 3.6 更新信号状态

**请求**:
```
PUT /api/signal/status/{id}?status=HANDLED&note=已处理
```

### 3.7 删除信号

**请求**:
```
DELETE /api/signal/{id}
```

### 3.8 获取最近信号

**请求**:
```
GET /api/signal/recent/{stockCode}?limit=10
```

### 3.9 测试通知渠道

**请求**:
```
POST /api/signal/test-channel?channel=EMAIL
```

## 4. 错误响应

所有API在发生错误时返回统一格式：

```json
{
  "timestamp": "2024-01-15T15:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "具体错误信息",
  "path": "/api/xxx"
}
```