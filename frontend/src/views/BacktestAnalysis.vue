<template>
  <div class="backtest-analysis">
    <!-- 回测参数配置 -->
    <el-card class="config-card">
      <template #header>
        <span>回测参数配置</span>
      </template>
      <el-form :model="backtestParams" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="股票代码">
              <el-input v-model="backtestParams.stockCode" placeholder="输入股票代码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="策略类型">
              <el-select v-model="backtestParams.strategyType" style="width: 100%;">
                <el-option label="均线金叉策略" value="MA_GOLDEN_CROSS" />
                <el-option label="MACD交叉策略" value="MACD_CROSS" />
                <el-option label="KDJ超卖策略" value="KDJ_OVERSOLD" />
                <el-option label="布林带策略" value="BOLL_BAND" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="初始资金">
              <el-input-number v-model="backtestParams.initialCapital" :min="10000" :step="10000" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="开始日期">
              <el-date-picker v-model="backtestParams.startDate" type="date" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束日期">
              <el-date-picker v-model="backtestParams.endDate" type="date" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item>
              <el-button type="primary" @click="runBacktest" :loading="running">
                <el-icon><VideoPlay /></el-icon>
                开始回测
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 回测结果 -->
    <el-card v-if="backtestResult" class="result-card">
      <template #header>
        <span>回测结果</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">总收益率</div>
            <div class="result-value" :class="backtestResult.totalReturn >= 0 ? 'positive' : 'negative'">
              {{ backtestResult.totalReturn }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">年化收益率</div>
            <div class="result-value" :class="backtestResult.annualReturn >= 0 ? 'positive' : 'negative'">
              {{ backtestResult.annualReturn }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">最大回撤</div>
            <div class="result-value negative">{{ backtestResult.maxDrawdown }}%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">夏普比率</div>
            <div class="result-value">{{ backtestResult.sharpeRatio }}</div>
          </div>
        </el-col>
      </el-row>
      <el-divider />
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">总交易次数</div>
            <div class="result-value">{{ backtestResult.totalTrades }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">胜率</div>
            <div class="result-value">{{ backtestResult.winRate }}%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">平均盈利</div>
            <div class="result-value positive">{{ backtestResult.avgProfit }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="result-item">
            <div class="result-label">平均亏损</div>
            <div class="result-value negative">{{ backtestResult.avgLoss }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 历史回测记录 -->
    <el-card class="history-card">
      <template #header>
        <span>历史回测记录</span>
      </template>
      <el-table :data="historyResults" style="width: 100%" size="small">
        <el-table-column prop="stockCode" label="股票代码" width="100" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="totalReturn" label="总收益率" width="100">
          <template #default="{ row }">
            <span :class="row.totalReturn >= 0 ? 'positive' : 'negative'">
              {{ row.totalReturn }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="annualReturn" label="年化收益" width="100" />
        <el-table-column prop="maxDrawdown" label="最大回撤" width="100" />
        <el-table-column prop="sharpeRatio" label="夏普比率" width="100" />
        <el-table-column prop="winRate" label="胜率" width="80" />
        <el-table-column prop="backtestTime" label="回测时间" width="160" />
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { runBacktest as runBacktestApi, getBacktestResults } from '@/api/stock'

const running = ref(false)
const backtestResult = ref<any>(null)
const historyResults = ref<any[]>([])

const backtestParams = ref({
  stockCode: '600519',
  strategyType: 'MA_GOLDEN_CROSS',
  initialCapital: 100000,
  startDate: new Date(Date.now() - 365 * 24 * 60 * 60 * 1000),
  endDate: new Date()
})

// 运行回测
const runBacktest = async () => {
  if (!backtestParams.value.stockCode) {
    ElMessage.warning('请输入股票代码')
    return
  }

  running.value = true
  try {
    const result = await runBacktestApi({
      stockCode: backtestParams.value.stockCode,
      strategyType: backtestParams.value.strategyType,
      initialCapital: backtestParams.value.initialCapital,
      startDate: backtestParams.value.startDate.toISOString().split('T')[0],
      endDate: backtestParams.value.endDate.toISOString().split('T')[0]
    })
    backtestResult.value = result
    ElMessage.success('回测完成')
    loadHistoryResults()
  } catch (error) {
    ElMessage.error('回测失败')
  } finally {
    running.value = false
  }
}

// 查看详情
const viewDetail = (row: any) => {
  backtestResult.value = row
}

// 加载历史记录
const loadHistoryResults = async () => {
  try {
    const data = await getBacktestResults(backtestParams.value.stockCode)
    historyResults.value = data as any[]
  } catch (error) {
    console.error('加载历史记录失败:', error)
  }
}

onMounted(() => {
  loadHistoryResults()
})
</script>

<style scoped>
.backtest-analysis {
  padding: 20px;
}

.config-card, .result-card, .history-card {
  margin-bottom: 20px;
}

.result-item {
  text-align: center;
  padding: 10px;
}

.result-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.result-value {
  font-size: 24px;
  font-weight: bold;
}

.positive {
  color: #F56C6C;
}

.negative {
  color: #67C23A;
}
</style>