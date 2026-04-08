<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #409EFF;">
            <el-icon><Database /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.stockCount }}</div>
            <div class="stat-label">股票数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #67C23A;">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.dataRecords }}</div>
            <div class="stat-label">数据记录</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #E6A23C;">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.strategyCount }}</div>
            <div class="stat-label">运行策略</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #F56C6C;">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.signalCount }}</div>
            <div class="stat-label">今日信号</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统状态 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
              <el-button type="primary" size="small" @click="refreshStatus">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="数据库状态">
              <el-tag :type="systemStatus.dbConnected ? 'success' : 'danger'">
                {{ systemStatus.dbConnected ? '已连接' : '未连接' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="数据源状态">
              <el-tag :type="systemStatus.dataSourceOk ? 'success' : 'danger'">
                {{ systemStatus.dataSourceOk ? '正常' : '异常' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最近采集时间">
              {{ systemStatus.lastCollectTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="数据完整性">
              {{ systemStatus.dataIntegrity || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <span>最近信号提醒</span>
          </template>
          <el-table :data="recentSignals" style="width: 100%" size="small">
            <el-table-column prop="stockCode" label="股票代码" width="100" />
            <el-table-column prop="stockName" label="股票名称" width="100" />
            <el-table-column prop="signalType" label="信号类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getSignalTypeColor(row.signalType)" size="small">
                  {{ getSignalTypeText(row.signalType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="triggerCondition" label="触发条件" show-overflow-tooltip />
            <el-table-column prop="signalTime" label="触发时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="section-card">
      <template #header>
        <span>快捷操作</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" size="large" @click="$router.push('/data')">
            <el-icon><Database /></el-icon>
            数据采集
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" size="large" @click="$router.push('/strategy')">
            <el-icon><TrendCharts /></el-icon>
            策略研究
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" size="large" @click="$router.push('/backtest')">
            <el-icon><DataAnalysis /></el-icon>
            运行回测
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="info" size="large" @click="$router.push('/signal')">
            <el-icon><Bell /></el-icon>
            查看信号
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSystemStatus, getSignalList } from '@/api/stock'

// 统计数据
const stats = ref({
  stockCount: 0,
  dataRecords: 0,
  strategyCount: 0,
  signalCount: 0
})

// 系统状态
const systemStatus = ref({
  dbConnected: false,
  dataSourceOk: false,
  lastCollectTime: '',
  dataIntegrity: ''
})

// 最近信号
const recentSignals = ref<any[]>([])

// 刷新状态
const refreshStatus = async () => {
  try {
    const status = await getSystemStatus()
    systemStatus.value = status as any
  } catch (error) {
    console.error('获取系统状态失败:', error)
  }
}

// 获取信号类型文本
const getSignalTypeText = (type: string) => {
  const map: Record<string, string> = {
    BUY: '买入',
    SELL: '卖出',
    RISK_WARNING: '风险预警'
  }
  return map[type] || type
}

// 获取信号类型颜色
const getSignalTypeColor = (type: string) => {
  const map: Record<string, string> = {
    BUY: 'success',
    SELL: 'danger',
    RISK_WARNING: 'warning'
  }
  return map[type] || 'info'
}

onMounted(async () => {
  await refreshStatus()
  try {
    const signals = await getSignalList()
    recentSignals.value = (signals as any[]).slice(0, 5)
  } catch (error) {
    console.error('获取信号失败:', error)
  }
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
}

.stat-icon .el-icon {
  font-size: 30px;
  color: #fff;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.section-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>