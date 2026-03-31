// src/router/index.js

import { createRouter, createWebHistory } from 'vue-router'
// 导入我们的 auth store
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard' // 现在让首页重定向到仪表盘
    },
    // --- 创建一个新的受保护的路由 ---
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: { requiresAuth: true } // meta 字段，标记这个路由需要认证
    },
    // -----------------------------
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue')
    }
  ]
})

// --- 定义全局前置路由守卫 ---
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // 检查路由是否需要认证
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // 如果需要认证但用户未登录，则重定向到登录页面
    next({ name: 'login' })
  } else {
    // 否则，允许继续导航
    next()
  }
})

export default router