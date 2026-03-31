<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

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
    <button @click="handleLogout">登出</button>
  </div>
</template>

<style scoped>
button {
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #f44336; /* 红色背景以示区别 */
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
button:hover {
  background-color: #d32f2f;
}
</style>