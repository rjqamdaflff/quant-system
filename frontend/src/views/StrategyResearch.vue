<template>
  <div class="strategy-research">
    <!-- 股票选择 -->
    <el-card class="select-card">
      <el-form :inline="true">
        <el-form-item label="股票代码">
          <el-input v-model="stockCode" placeholder="输入股票代码" style="width: 150px;" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadChartData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 技术指标选择 -->
    <el-card class="indicator-card">
      <template #header>
        <span>技术指标</span>
      </template>
      <el-checkbox-group v-model="selectedIndicators">
        <el-checkbox label="MA5">MA5</el-checkbox>
        <el-checkbox label="MA10">MA10</el-checkbox>
        <el-checkbox label="MA20">MA20</el-checkbox>
        <el-checkbox label="MA60">MA60</el-checkbox>
        <el-checkbox label="MACD">MACD</el-checkbox>
        <el-checkbox label="KDJ">KDJ</el-checkbox>
        <el-checkbox label="RSI">RSI</el-checkbox>
        <el-checkbox label="BOLL">布林带</el-checkbox>
      </el-checkbox-group>
    </el-card>

    <!-- K线图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>K线图 - {{ stockCode }}</span>
        </div>
      </template>
      <div ref="chartContainer" class="chart-container"></div>
    </el-card>

    <!-- 策略配置 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>内置策略</span>
          </template>
          <el-table :data="strategyTypes" style="width: 100%" size="small">
            <el-table-column prop="name" label="策略名称" width="120" />
            <el-table-column prop="desc" label="策略说明" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="applyStrategy(row)">
                  应用
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>策略配置</span>
          </template>
          <el-form :model="strategyConfig" label-width="100px">
            <el-form-item label="策略名称">
              <el-input v-model="strategyConfig.strategyName" />
            </el-form-item>
            <el-form-item label="策略类型">
              <el-select v-model="strategyConfig.strategyType" style="width: 100%;">
                <el-option label="均线金叉策略" value="MA_GOLDEN_CROSS" />
                <el-option label="MACD交叉策略" value="MACD_CROSS" />
                <el-option label="KDJ超卖策略" value="KDJ_OVERSOLD" />
                <el-option label="布林带策略" value="BOLL_BAND" />
              </el-select>
            </el-form-item>
            <el-form-item label="止损比例">
              <el-input-number v-model="strategyConfig.stopLossPct" :min="0" :max="20" :step="0.5" />
              %
            </el-form-item>
            <el-form-item label="止盈比例">
              <el-input-number v-model="strategyConfig.stopProfitPct" :min="0" :max="50" :step="1" />
              %
            </el-form-item>
            <el-form-item label="启用提醒">
              <el-switch v-model="strategyConfig.alertEnabled" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveStrategy">保存策略</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDailyData, getStrategyTypes, createStrategyConfig } from '@/api/stock'

const stockCode = ref('600519')
const dateRange = ref<[Date, Date] | null>(null)
const selectedIndicators = ref(['MA5', 'MA10', 'MA20'])
const chartContainer = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const strategyTypes = ref<any[]>([])
const strategyConfig = ref({
  strategyName: '',
  strategyType: 'MA_GOLDEN_CROSS',
  stopLossPct: 5,
  stopProfitPct: 10,
  alertEnabled: true
})

// 加载K线图
const loadChartData = async () => {
  if (!stockCode.value) {
    ElMessage.warning('请输入股票代码')
    return
  }

  try {
    // 获取日线数据
    const data = await getDailyData(stockCode.value)
    renderChart(data as any[])
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

// 渲染K线图
const renderChart = (data: any[]) => {
  if (!chartContainer.value) return

  if (!chart) {
    chart = echarts.init(chartContainer.value)
  }

  const dates = data.map(d => d.tradeDate)
  const ohlc = data.map(d => [d.open, d.close, d.low, d.high])
  const volumes = data.map(d => d.volume)

  const option = {
    title: { text: `${stockCode.value} K线图`, left: 'center' },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: { data: ['K线', '成交量'], top: 30 },
    grid: [
      { left: '10%', right: '8%', top: 60, height: '50%' },
      { left: '10%', right: '8%', top: '70%', height: '20%' }
    ],
    xAxis: [
      { type: 'category', data: dates, gridIndex: 0 },
      { type: 'category', data: dates, gridIndex: 1 }
    ],
    yAxis: [
      { scale: true, gridIndex: 0 },
      { scale: true, gridIndex: 1, splitLine: { show: false } }
    ],
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 1], start: 50, end: 100 },
      { type: 'slider', xAxisIndex: [0, 1], bottom: 10 }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: ohlc,
        xAxisIndex: 0,
        yAxisIndex: 0
      },
      {
        name: '成交量',
        type: 'bar',
        data: volumes,
        xAxisIndex: 1,
        yAxisIndex: 1
      }
    ]
  }

  chart.setOption(option)
}

// 应用策略
const applyStrategy = (strategy: any) => {
  strategyConfig.value.strategyType = strategy.code
  strategyConfig.value.strategyName = strategy.name
  ElMessage.success(`已选择策略: ${strategy.name}`)
}

// 保存策略
const saveStrategy = async () => {
  if (!strategyConfig.value.strategyName) {
    ElMessage.warning('请输入策略名称')
    return
  }

  try {
    await createStrategyConfig({
      ...strategyConfig.value,
      targetStocks: stockCode.value
    })
    ElMessage.success('策略保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 加载策略类型
const loadStrategyTypes = async () => {
  try {
    const data = await getStrategyTypes()
    strategyTypes.value = data as any[]
  } catch (error) {
    console.error('加载策略类型失败:', error)
  }
}

onMounted(() => {
  loadStrategyTypes()
  // 设置默认日期范围
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 3)
  dateRange.value = [start, end]
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped>
.strategy-research {
  padding: 20px;
}

.select-card, .indicator-card, .chart-card {
  margin-bottom: 20px;
}

.chart-container {
  width: 100%;
  height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>