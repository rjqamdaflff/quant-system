import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器
api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// ========== 数据采集 API ==========

/** 获取股票列表 */
export const getStockList = () => api.get('/collect/stocks')

/** 获取系统状态 */
export const getSystemStatus = () => api.get('/collect/status')

/** 采集单只股票日线数据 */
export const collectDailyData = (stockCode: string) => api.post(`/collect/daily/${stockCode}`)

/** 批量采集日线数据 */
export const collectDailyBatch = (stockCodes: string[]) =>
  api.post('/collect/daily/batch', stockCodes)

/** 查询日线数据 */
export const getDailyData = (stockCode: string, startDate?: string, endDate?: string) =>
  api.get(`/collect/daily/${stockCode}`, { params: { startDate, endDate } })

/** 验证数据 */
export const validateData = (stockCode: string) => api.get(`/collect/validate/${stockCode}`)

// ========== 策略分析 API ==========

/** 获取策略类型列表 */
export const getStrategyTypes = () => api.get('/strategy/types')

/** 获取策略配置列表 */
export const getStrategyList = () => api.get('/strategy/list')

/** 创建策略配置 */
export const createStrategyConfig = (data: any) => api.post('/strategy/config', data)

/** 更新策略配置 */
export const updateStrategyConfig = (id: number, data: any) =>
  api.put(`/strategy/config/${id}`, data)

/** 删除策略配置 */
export const deleteStrategyConfig = (id: number) => api.delete(`/strategy/config/${id}`)

/** 执行回测 */
export const runBacktest = (data: {
  stockCode: string
  startDate: string
  endDate: string
  initialCapital?: number
  strategyType?: string
}) => api.post('/strategy/backtest', data)

/** 获取回测结果 */
export const getBacktestResults = (stockCode: string) =>
  api.get(`/strategy/backtest/${stockCode}`)

// ========== 信号提醒 API ==========

/** 获取信号列表 */
export const getSignalList = (stockCode?: string, status?: string) =>
  api.get('/signal/list', { params: { stockCode, status } })

/** 生成信号 */
export const generateSignals = (stockCode: string, stockName?: string, days?: number) =>
  api.post('/signal/generate', null, { params: { stockCode, stockName, days } })

/** 获取待发送信号 */
export const getPendingSignals = () => api.get('/signal/pending')

/** 发送信号 */
export const sendSignal = (id: number, channels?: string) =>
  api.post(`/signal/send/${id}`, null, { params: { channels } })

/** 批量发送信号 */
export const sendAllPendingSignals = () => api.post('/signal/send-all')

/** 更新信号状态 */
export const updateSignalStatus = (id: number, status: string, note?: string) =>
  api.put(`/signal/status/${id}`, null, { params: { status, note } })

/** 删除信号 */
export const deleteSignal = (id: number) => api.delete(`/signal/${id}`)

/** 获取最近信号 */
export const getRecentSignals = (stockCode: string, limit?: number) =>
  api.get(`/signal/recent/${stockCode}`, { params: { limit } })

/** 测试通知渠道 */
export const testNotificationChannel = (channel: string) =>
  api.post('/signal/test-channel', null, { params: { channel } })

export default api