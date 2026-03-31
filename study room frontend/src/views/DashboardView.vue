<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const displayName = computed(() => authStore.user?.username || authStore.user?.email || '用户')
const userId = computed(() => authStore.user?.id ?? '-')
const userEmail = computed(() => authStore.user?.email || '-')

const handleLogout = () => {
  authStore.clearAuth()
  alert('您已成功登出。')
  router.push({ name: 'login' })
}
</script>

<template>
  <div>
    <h1>欢迎来到仪表盘</h1>
    <p>这是一个受保护的页面，只有登录用户才能看到。</p>
    <p>当前登录用户：{{ displayName }}</p>
    <p>用户 ID：{{ userId }}</p>
    <p>邮箱：{{ userEmail }}</p>
    <button @click="handleLogout">登出</button>
  </div>
</template>

<style scoped>
button {
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
button:hover {
  background-color: #d32f2f;
}
</style>
