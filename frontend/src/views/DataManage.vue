<template>
  <div class="data-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="股票代码">
          <el-input v-model="searchForm.stockCode" placeholder="输入股票代码" clearable />
        </el-form-item>
        <el-form-item label="股票名称">
          <el-input v-model="searchForm.stockName" placeholder="输入股票名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="action-card">
      <el-button type="primary" @click="handleCollectAll">
        <el-icon><Download /></el-icon>
        批量采集
      </el-button>
      <el-button type="success" @click="handleValidateAll">
        <el-icon><Check /></el-icon>
        数据验证
      </el-button>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="stockList" style="width: 100%" v-loading="loading">
        <el-table-column prop="stockCode" label="股票代码" width="120" />
        <el-table-column prop="stockName" label="股票名称" width="150" />
        <el-table-column prop="industry" label="所属行业" width="120" />
        <el-table-column prop="listDate" label="上市日期" width="120" />
        <el-table-column prop="dataStatus" label="数据状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.dataStatus === 'COMPLETE' ? 'success' : 'warning'" size="small">
              {{ row.dataStatus === 'COMPLETE' ? '完整' : '待更新' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="数据条数" width="100" />
        <el-table-column prop="latestDate" label="最新日期" width="120" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleCollect(row)">
              采集
            </el-button>
            <el-button size="small" type="success" @click="handleViewData(row)">
              查看
            </el-button>
            <el-button size="small" type="warning" @click="handleValidate(row)">
              验证
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 数据详情对话框 -->
    <el-dialog v-model="dataDialogVisible" title="数据详情" width="80%">
      <el-table :data="dailyData" style="width: 100%">
        <el-table-column prop="tradeDate" label="交易日期" width="120" />
        <el-table-column prop="open" label="开盘价" width="100" />
        <el-table-column prop="close" label="收盘价" width="100" />
        <el-table-column prop="high" label="最高价" width="100" />
        <el-table-column prop="low" label="最低价" width="100" />
        <el-table-column prop="volume" label="成交量" width="120" />
        <el-table-column prop="amount" label="成交额" width="120" />
        <el-table-column prop="changePct" label="涨跌幅" width="100">
          <template #default="{ row }">
            <span :class="row.changePct >= 0 ? 'text-red' : 'text-green'">
              {{ row.changePct }}%
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStockList, collectDailyData, getDailyData, validateData } from '@/api/stock'

const loading = ref(false)
const stockList = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const searchForm = ref({
  stockCode: '',
  stockName: ''
})

const dataDialogVisible = ref(false)
const dailyData = ref<any[]>([])

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadStockList()
}

// 重置
const handleReset = () => {
  searchForm.value = { stockCode: '', stockName: '' }
  loadStockList()
}

// 加载股票列表
const loadStockList = async () => {
  loading.value = true
  try {
    const data = await getStockList()
    stockList.value = data as any[]
    total.value = stockList.value.length
  } catch (error) {
    console.error('加载股票列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 采集单只股票
const handleCollect = async (row: any) => {
  try {
    ElMessage.info(`开始采集 ${row.stockCode}...`)
    await collectDailyData(row.stockCode)
    ElMessage.success('采集完成')
    loadStockList()
  } catch (error) {
    ElMessage.error('采集失败')
  }
}

// 批量采集
const handleCollectAll = () => {
  ElMessage.info('批量采集功能开发中...')
}

// 数据验证
const handleValidate = async (row: any) => {
  try {
    const result = await validateData(row.stockCode)
    ElMessage.success('数据验证完成')
  } catch (error) {
    ElMessage.error('验证失败')
  }
}

// 批量验证
const handleValidateAll = () => {
  ElMessage.info('批量验证功能开发中...')
}

// 查看数据
const handleViewData = async (row: any) => {
  try {
    const data = await getDailyData(row.stockCode)
    dailyData.value = data as any[]
    dataDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

// 分页
const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadStockList()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadStockList()
}

onMounted(() => {
  loadStockList()
})
</script>

<style scoped>
.data-manage {
  padding: 20px;
}

.search-card, .action-card, .table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.text-red {
  color: #F56C6C;
}

.text-green {
  color: #67C23A;
}
</style>