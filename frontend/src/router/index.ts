import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue')
    },
    {
      path: '/data',
      name: 'DataManage',
      component: () => import('@/views/DataManage.vue')
    },
    {
      path: '/strategy',
      name: 'StrategyResearch',
      component: () => import('@/views/StrategyResearch.vue')
    },
    {
      path: '/backtest',
      name: 'BacktestAnalysis',
      component: () => import('@/views/BacktestAnalysis.vue')
    },
    {
      path: '/signal',
      name: 'SignalAlert',
      component: () => import('@/views/SignalAlert.vue')
    }
  ]
})

export default router