<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getRooms } from '@/services/reservationApi'

const rooms = ref([])
const loading = ref(false)
const errorMessage = ref('')

const activeRoomsCount = computed(() => rooms.value.filter((room) => room.status === 1).length)

async function loadRooms() {
  loading.value = true
  errorMessage.value = ''

  try {
    const { data } = await getRooms()
    rooms.value = data
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '加载自习室列表失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function roomStatusText(status) {
  return status === 1 ? '开放中' : '未开放'
}

onMounted(() => {
  loadRooms()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">自习室管理</p>
        <h1>自习室列表</h1>
        <p>查看开放中的自习室，并进入房间详情选择时段与座位发起预约。</p>
      </div>
      <div class="summary-card">
        <strong>{{ activeRoomsCount }}</strong>
        <span>开放中的自习室</span>
      </div>
    </header>

    <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
    <div v-else-if="loading" class="feedback">正在加载自习室列表...</div>
    <div v-else-if="!rooms.length" class="feedback">当前没有可展示的自习室。</div>

    <div v-else class="room-grid">
      <article v-for="room in rooms" :key="room.id" class="room-card">
        <div class="room-card-header">
          <div>
            <h2>{{ room.name }}</h2>
            <p>{{ room.location || '暂无位置信息' }}</p>
          </div>
          <span :class="['status', { inactive: room.status !== 1 }]">{{ roomStatusText(room.status) }}</span>
        </div>

        <p class="description">{{ room.description || '暂无房间说明。' }}</p>

        <RouterLink :to="`/rooms/${room.id}`" class="action-link">查看详情并预约</RouterLink>
      </article>
    </div>
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: stretch;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

.summary-card {
  min-width: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border-radius: 14px;
  color: #1d4ed8;
}

.summary-card strong {
  font-size: 36px;
}

.feedback {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 20px 24px;
  color: #374151;
}

.feedback.error {
  border-color: #fecaca;
  color: #b91c1c;
  background: #fef2f2;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

.room-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
}

.room-card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.room-card h2 {
  margin: 0 0 8px;
}

.room-card p {
  margin: 0;
  color: #4b5563;
}

.description {
  min-height: 44px;
}

.status {
  background: #dcfce7;
  color: #166534;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 14px;
  white-space: nowrap;
}

.status.inactive {
  background: #f3f4f6;
  color: #4b5563;
}

.action-link {
  display: inline-flex;
  align-self: flex-start;
  color: #2563eb;
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
