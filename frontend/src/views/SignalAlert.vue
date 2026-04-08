<template>
  <div class="signal-alert">
    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="股票代码">
          <el-input v-model="filters.stockCode" placeholder="输入股票代码" clearable />
        </el-form-item>
        <el-form-item label="信号类型">
          <el-select v-model="filters.signalType" placeholder="全部" clearable>
            <el-option label="买入信号" value="BUY" />
            <el-option label="卖出信号" value="SELL" />
            <el-option label="风险预警" value="RISK_WARNING" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable>
            <el-option label="待发送" value="PENDING" />
            <el-option label="已发送" value="SENT" />
            <el-option label="已处理" value="HANDLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSignals">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button type="success" @click="generateNewSignals">
            <el-icon><Plus /></el-icon>
            生成信号
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 信号列表 -->
    <el-card class="signal-card">
      <template #header>
        <div class="card-header">
          <span>信号列表</span>
          <el-button type="primary" size="small" @click="sendAllPending">
            批量发送待处理信号
          </el-button>
        </div>
      </template>
      <el-table :data="signalList" style="width: 100%" v-loading="loading">
        <el-table-column prop="stockCode" label="股票代码" width="100" />
        <el-table-column prop="stockName" label="股票名称" width="100" />
        <el-table-column prop="signalType" label="信号类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getSignalTypeColor(row.signalType)" size="small">
              {{ getSignalTypeText(row.signalType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerPrice" label="触发价格" width="100" />
        <el-table-column prop="triggerCondition" label="触发条件" show-overflow-tooltip />
        <el-table-column prop="signalStrength" label="信号强度" width="100">
          <template #default="{ row }">
            <el-tag :type="getStrengthColor(row.signalStrength)" size="small">
              {{ row.signalStrength }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议操作" width="80">
          <template #default="{ row }">
            <el-tag :type="getSuggestionColor(row.suggestion)" size="small">
              {{ getSuggestionText(row.suggestion) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertStatus" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.alertStatus)" size="small">
              {{ getStatusText(row.alertStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="signalTime" label="触发时间" width="160" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="sendSignal(row)" :disabled="row.alertStatus !== 'PENDING'">
              发送
            </el-button>
            <el-button size="small" type="success" @click="markHandled(row)" :disabled="row.alertStatus === 'HANDLED'">
              已处理
            </el-button>
            <el-button size="small" type="danger" @click="removeSignal(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 生成信号对话框 -->
    <el-dialog v-model="generateDialogVisible" title="生成信号" width="500px">
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="股票代码">
          <el-input v-model="generateForm.stockCode" placeholder="输入股票代码" />
        </el-form-item>
        <el-form-item label="股票名称">
          <el-input v-model="generateForm.stockName" placeholder="输入股票名称" />
        </el-form-item>
        <el-form-item label="分析天数">
          <el-input-number v-model="generateForm.days" :min="30" :max="250" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSignalList,
  generateSignals,
  sendSignal as sendSignalApi,
  sendAllPendingSignals,
  updateSignalStatus,
  deleteSignal
} from '@/api/stock'

const loading = ref(false)
const signalList = ref<any[]>([])

const filters = ref({
  stockCode: '',
  signalType: '',
  status: ''
})

const generateDialogVisible = ref(false)
const generateForm = ref({
  stockCode: '',
  stockName: '',
  days: 60
})

// 加载信号列表
const loadSignals = async () => {
  loading.value = true
  try {
    const data = await getSignalList(filters.value.stockCode, filters.value.status)
    signalList.value = data as any[]
  } catch (error) {
    console.error('加载信号失败:', error)
  } finally {
    loading.value = false
  }
}

// 生成新信号
const generateNewSignals = () => {
  generateDialogVisible.value = true
}

// 确认生成
const confirmGenerate = async () => {
  if (!generateForm.value.stockCode) {
    ElMessage.warning('请输入股票代码')
    return
  }

  try {
    await generateSignals(
      generateForm.value.stockCode,
      generateForm.value.stockName,
      generateForm.value.days
    )
    ElMessage.success('信号生成成功')
    generateDialogVisible.value = false
    loadSignals()
  } catch (error) {
    ElMessage.error('生成失败')
  }
}

// 发送单个信号
const sendSignal = async (row: any) => {
  try {
    await sendSignalApi(row.id, 'EMAIL,DINGTALK')
    ElMessage.success('发送成功')
    loadSignals()
  } catch (error) {
    ElMessage.error('发送失败')
  }
}

// 批量发送
const sendAllPending = async () => {
  try {
    await ElMessageBox.confirm('确认发送所有待处理信号?', '提示')
    await sendAllPendingSignals()
    ElMessage.success('批量发送完成')
    loadSignals()
  } catch (error) {
    // 用户取消
  }
}

// 标记已处理
const markHandled = async (row: any) => {
  try {
    await updateSignalStatus(row.id, 'HANDLED')
    ElMessage.success('已标记为处理')
    loadSignals()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除信号
const removeSignal = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该信号?', '提示')
    await deleteSignal(row.id)
    ElMessage.success('删除成功')
    loadSignals()
  } catch (error) {
    // 用户取消
  }
}

// 辅助方法
const getSignalTypeText = (type: string) => ({ BUY: '买入', SELL: '卖出', RISK_WARNING: '风险预警' }[type] || type)
const getSignalTypeColor = (type: string) => ({ BUY: 'success', SELL: 'danger', RISK_WARNING: 'warning' }[type] || 'info')
const getStrengthColor = (strength: string) => ({ STRONG: 'danger', MEDIUM: 'warning', WEAK: 'info' }[strength] || 'info')
const getSuggestionText = (suggestion: string) => ({ BUY: '买入', SELL: '卖出', HOLD: '持有', WATCH: '观察' }[suggestion] || suggestion)
const getSuggestionColor = (suggestion: string) => ({ BUY: 'success', SELL: 'danger', HOLD: 'info', WATCH: 'warning' }[suggestion] || 'info')
const getStatusText = (status: string) => ({ PENDING: '待发送', SENT: '已发送', HANDLED: '已处理', FAILED: '发送失败' }[status] || status)
const getStatusColor = (status: string) => ({ PENDING: 'warning', SENT: 'success', HANDLED: 'info', FAILED: 'danger' }[status] || 'info')

onMounted(() => {
  loadSignals()
})
</script>

<style scoped>
.signal-alert {
  padding: 20px;
}

.filter-card, .signal-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>