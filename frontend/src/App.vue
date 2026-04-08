<template>
  <el-config-provider :locale="zhCn">
    <el-container class="app-container">
      <!-- 侧边栏 -->
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <h1>Quant System</h1>
          <p>股票量化交易系统</p>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/">
            <el-icon><DataLine /></el-icon>
            <span>首页仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/data">
            <el-icon><Database /></el-icon>
            <span>数据管理</span>
          </el-menu-item>
          <el-menu-item index="/strategy">
            <el-icon><TrendCharts /></el-icon>
            <span>策略研究</span>
          </el-menu-item>
          <el-menu-item index="/backtest">
            <el-icon><DataAnalysis /></el-icon>
            <span>回测分析</span>
          </el-menu-item>
          <el-menu-item index="/signal">
            <el-icon><Bell /></el-icon>
            <span>信号提醒</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <el-header class="header">
          <div class="header-title">{{ pageTitle }}</div>
          <div class="header-actions">
            <el-button type="primary" size="small" @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const route = useRoute()

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 页面标题
const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/': '首页仪表盘',
    '/data': '数据管理',
    '/strategy': '策略研究',
    '/backtest': '回测分析',
    '/signal': '信号提醒'
  }
  return titles[route.path] || '股票量化交易系统'
})

// 刷新数据
const refreshData = () => {
  window.location.reload()
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

.app-container {
  height: 100%;
}

.sidebar {
  background-color: #304156;
  overflow-y: auto;
}

.logo {
  height: 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  border-bottom: 1px solid #3a4a5d;
}

.logo h1 {
  font-size: 18px;
  margin-bottom: 5px;
}

.logo p {
  font-size: 12px;
  color: #8a9aaa;
}

.sidebar-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>