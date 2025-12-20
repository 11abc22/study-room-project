<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const displayName = computed(() => authStore.user?.username || authStore.user?.email || '用户')
const isAdmin = computed(() => authStore.user?.username === 'admin')
const todaySummary = computed(() => {
  const items = [
    { title: '自习室列表', description: '查看可用自习室、位置与后续可扩展的容量信息。', action: '进入列表', routeName: 'rooms' },
    { title: '我的预约', description: '查看当前账号的预约记录，下一阶段接入真实预约明细。', action: '查看预约', routeName: 'my-reservations' }
  ]
  if (isAdmin.value) {
    items.push(
      { title: '预约管理', description: '管理员：查看所有预约记录，支持筛选、取消和修改状态。', action: '进入管理', routeName: 'AdminReservations' },
      { title: '留言管理', description: '管理员：查看和删除所有座位留言。', action: '进入管理', routeName: 'AdminComments' }
    )
  }
  return items
})

const handleLogout = () => {
  authStore.clearAuth()
  router.push({ name: 'login' })
}

const goTo = (routeName) => {
  router.push({ name: routeName })
}
</script>

<template>
  <section class="dashboard">
    <div class="hero-card">
      <div>
        <p class="eyebrow">业务入口</p>
        <h1>欢迎回来，{{ displayName }}</h1>
        <p class="hero-text">
          当前阶段已完成系统稳定化与业务骨架整理。你可以从这里进入自习室列表、我的预约，后续功能会在此基础上逐步接入。
        </p>
      </div>
      <button class="logout-button" @click="handleLogout">退出登录</button>
    </div>

    <div class="entry-grid">
      <article v-for="item in todaySummary" :key="item.routeName" class="entry-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <button @click="goTo(item.routeName)">{{ item.action }}</button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-card,
.entry-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

h1,
h2,
p {
  margin-top: 0;
}

.hero-text,
.entry-card p {
  color: #4b5563;
  line-height: 1.6;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

button {
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  background: #2563eb;
  color: #ffffff;
  cursor: pointer;
}

.logout-button {
  background: #ef4444;
}
</style>
