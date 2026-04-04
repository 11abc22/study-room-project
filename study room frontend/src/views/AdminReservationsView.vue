<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAllReservations, deleteReservation, updateReservationStatus } from '@/services/adminApi'

const router = useRouter()

const reservations = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const cancellingId = ref(null)
const updatingId = ref(null)

const filterUserId = ref('')
const filterStatus = ref('')
const filterDate = ref('')

async function loadReservations() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  const params = {}
  if (filterUserId.value) params.userId = filterUserId.value
  if (filterStatus.value !== '') params.status = filterStatus.value
  if (filterDate.value) params.date = filterDate.value

  try {
    const { data } = await getAllReservations(params)
    reservations.value = data
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '加载预约列表失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function formatTime(time) {
  if (!time) return ''
  return String(time).slice(0, 5)
}

function statusText(status) {
  const map = { 0: '已取消（管理员）', 1: '已预约', 2: '已取消' }
  return map[status] || '未知'
}

function statusClass(status) {
  if (status === 1) return 'active'
  return 'cancelled'
}

async function handleCancel(id) {
  successMessage.value = ''
  errorMessage.value = ''
  cancellingId.value = id

  try {
    const { data } = await deleteReservation(id)
    successMessage.value = data.message || '预约已取消'
    await loadReservations()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '取消预约失败，请稍后重试。'
  } finally {
    cancellingId.value = null
  }
}

async function handleUpdateStatus(id, newStatus) {
  successMessage.value = ''
  errorMessage.value = ''
  updatingId.value = id

  try {
    const { data } = await updateReservationStatus(id, newStatus)
    successMessage.value = data.message || '状态已更新'
    await loadReservations()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '更新状态失败，请稍后重试。'
  } finally {
    updatingId.value = null
  }
}

function goBack() {
  router.push({ name: 'dashboard' })
}

onMounted(() => {
  loadReservations()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>预约管理</h1>
        <p>查看所有预约记录，支持筛选和操作。</p>
      </div>
      <button class="ghost-button" @click="goBack">返回首页</button>
    </header>

    <div class="panel">
      <h3>筛选条件</h3>
      <div class="filter-grid">
        <label>
          <span>用户 ID</span>
          <input v-model="filterUserId" type="number" placeholder="输入用户 ID" />
        </label>
        <label>
          <span>状态</span>
          <select v-model="filterStatus">
            <option value="">全部</option>
            <option value="1">已预约</option>
            <option value="0">已取消（管理员）</option>
            <option value="2">已取消</option>
          </select>
        </label>
        <label>
          <span>日期</span>
          <input v-model="filterDate" type="date" />
        </label>
      </div>
      <div class="actions">
        <button class="primary-button" @click="loadReservations">查询</button>
        <button class="ghost-button" @click="filterUserId = ''; filterStatus = ''; filterDate = ''; loadReservations()">重置</button>
      </div>
    </div>

    <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>
    <div v-if="loading" class="feedback">正在加载预约列表...</div>
    <div v-else-if="!reservations.length" class="feedback">没有找到符合条件的预约记录。</div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户</th>
            <th>自习室</th>
            <th>座位</th>
            <th>日期</th>
            <th>时间段</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in reservations" :key="r.id">
            <td>{{ r.id }}</td>
            <td>
              <span class="user-info">{{ r.username || '用户' + r.userId }}</span>
              <span class="user-id">（ID: {{ r.userId }}）</span>
            </td>
            <td>{{ r.roomName || '未知' }}</td>
            <td>{{ r.seatCode || '未知' }}</td>
            <td>{{ r.reserveDate }}</td>
            <td>{{ formatTime(r.startTime) }} - {{ formatTime(r.endTime) }}</td>
            <td><span :class="['status', statusClass(r.status)]">{{ statusText(r.status) }}</span></td>
            <td>
              <div class="action-group">
                <button
                  v-if="r.status === 1"
                  class="danger-button small"
                  :disabled="cancellingId === r.id"
                  @click="handleCancel(r.id)"
                >
                  {{ cancellingId === r.id ? '取消中...' : '取消预约' }}
                </button>
                <select
                  v-else
                  class="status-select"
                  :disabled="updatingId === r.id"
                  @change="handleUpdateStatus(r.id, Number($event.target.value)); $event.target.value = ''"
                >
                  <option value="">修改状态</option>
                  <option value="1">设为已预约</option>
                  <option value="0">设为已取消</option>
                  <option value="2">设为用户取消</option>
                </select>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav class="admin-nav">
      <router-link :to="{ name: 'AdminComments' }" class="nav-link">管理留言 →</router-link>
    </nav>
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.panel,
.feedback,
.table-wrapper {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

h1, h2, h3, p {
  margin-top: 0;
}

.panel h3 {
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #374151;
  font-weight: 500;
  font-size: 14px;
}

input, select {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.primary-button,
.ghost-button,
.danger-button {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.primary-button {
  background: #2563eb;
  color: #fff;
}

.ghost-button {
  background: #eff6ff;
  color: #1d4ed8;
}

.danger-button {
  background: #fee2e2;
  color: #b91c1c;
}

.danger-button.small {
  padding: 6px 12px;
  font-size: 13px;
}

.danger-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.feedback {
  color: #374151;
}

.feedback.error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.feedback.success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.table-wrapper {
  overflow-x: auto;
  padding: 0;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
  white-space: nowrap;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  position: sticky;
  top: 0;
}

.user-info {
  font-weight: 500;
}

.user-id {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 4px;
}

.status {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
}

.status.active {
  background: #dcfce7;
  color: #166534;
}

.status.cancelled {
  background: #f3f4f6;
  color: #4b5563;
}

.action-group {
  display: flex;
  gap: 8px;
}

.status-select {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 6px 10px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.admin-nav {
  display: flex;
  justify-content: flex-end;
}

.nav-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.nav-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .data-table th,
  .data-table td {
    padding: 8px 10px;
    font-size: 13px;
  }
}
</style>
