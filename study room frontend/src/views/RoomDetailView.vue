<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { createReservation, getRoomSeatStatus, getRooms } from '@/services/reservationApi'

const route = useRoute()
const roomId = computed(() => Number(route.params.id))

const room = ref(null)
const loadingRoom = ref(false)
const loadingSeats = ref(false)
const reservingSeatId = ref(null)
const roomError = ref('')
const seatError = ref('')
const successMessage = ref('')

const form = ref({
  reserveDate: '',
  startTime: '09:00',
  endTime: '11:00'
})

const seatStatusList = ref([])
const hasQueried = ref(false)

const availableSeats = computed(() =>
  seatStatusList.value.filter((seat) => seat.seatStatus === 1 && !seat.reserved)
)

function buildPayload(seatId = null) {
  return {
    roomId: roomId.value,
    seatId,
    reserveDate: form.value.reserveDate,
    startTime: form.value.startTime,
    endTime: form.value.endTime
  }
}

function validateTimeRange() {
  if (!form.value.reserveDate || !form.value.startTime || !form.value.endTime) {
    seatError.value = '请先完整选择预约日期、开始时间和结束时间。'
    return false
  }

  if (form.value.startTime >= form.value.endTime) {
    seatError.value = '开始时间必须早于结束时间。'
    return false
  }

  return true
}

async function loadRoom() {
  loadingRoom.value = true
  roomError.value = ''

  try {
    const { data } = await getRooms()
    room.value = data.find((item) => item.id === roomId.value) || null

    if (!room.value) {
      roomError.value = '未找到对应的自习室信息。'
    }
  } catch (error) {
    roomError.value = error.response?.data?.message || '加载房间信息失败，请稍后重试。'
  } finally {
    loadingRoom.value = false
  }
}

async function querySeatStatus() {
  successMessage.value = ''
  seatError.value = ''

  if (!validateTimeRange()) {
    return
  }

  loadingSeats.value = true
  hasQueried.value = true

  try {
    const { data } = await getRoomSeatStatus(roomId.value, buildPayload())
    seatStatusList.value = data
  } catch (error) {
    seatStatusList.value = []
    seatError.value = error.response?.data?.message || '查询座位状态失败，请稍后重试。'
  } finally {
    loadingSeats.value = false
  }
}

async function reserveSeat(seat) {
  successMessage.value = ''
  seatError.value = ''

  if (!validateTimeRange()) {
    return
  }

  reservingSeatId.value = seat.seatId

  try {
    const { data } = await createReservation(buildPayload(seat.seatId))
    successMessage.value = `${seat.seatCode} 预约成功。${data.message || ''}`.trim()
    await querySeatStatus()
  } catch (error) {
    seatError.value = error.response?.data?.message || '创建预约失败，请稍后重试。'
  } finally {
    reservingSeatId.value = null
  }
}

function seatStatusText(seat) {
  if (seat.seatStatus !== 1) {
    return '不可用'
  }

  return seat.reserved ? '已被预约' : '空闲可预约'
}

onMounted(() => {
  loadRoom()
})
</script>

<template>
  <section class="page">
    <div v-if="loadingRoom" class="feedback">正在加载房间信息...</div>
    <div v-else-if="roomError" class="feedback error">{{ roomError }}</div>

    <template v-else-if="room">
      <header class="page-header">
        <div>
          <p class="eyebrow">房间详情</p>
          <h1>{{ room.name }}</h1>
          <p>{{ room.location || '暂无位置信息' }}</p>
        </div>
        <div class="room-meta">
          <span :class="['status', { inactive: room.status !== 1 }]">
            {{ room.status === 1 ? '开放中' : '未开放' }}
          </span>
          <p>{{ room.description || '暂无房间说明。' }}</p>
        </div>
      </header>

      <section class="panel">
        <h2>选择预约时段</h2>
        <div class="form-grid">
          <label>
            <span>预约日期</span>
            <input v-model="form.reserveDate" type="date" />
          </label>
          <label>
            <span>开始时间</span>
            <input v-model="form.startTime" type="time" />
          </label>
          <label>
            <span>结束时间</span>
            <input v-model="form.endTime" type="time" />
          </label>
        </div>
        <div class="actions">
          <button class="primary-button" :disabled="loadingSeats || room.status !== 1" @click="querySeatStatus">
            {{ loadingSeats ? '查询中...' : '查询该时段座位状态' }}
          </button>
        </div>
      </section>

      <div v-if="seatError" class="feedback error">{{ seatError }}</div>
      <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>

      <section class="panel">
        <div class="section-header">
          <div>
            <h2>座位列表</h2>
            <p v-if="hasQueried">当前可预约座位：{{ availableSeats.length }} 个</p>
            <p v-else>请先选择时段并点击查询。</p>
          </div>
        </div>

        <div v-if="loadingSeats" class="feedback inline">正在加载座位状态...</div>
        <div v-else-if="hasQueried && !seatStatusList.length" class="feedback inline">当前时段暂无座位数据。</div>
        <div v-else-if="hasQueried" class="seat-grid">
          <article
            v-for="seat in seatStatusList"
            :key="seat.seatId"
            :class="['seat-card', { reserved: seat.reserved || seat.seatStatus !== 1 }]"
          >
            <div>
              <h3>{{ seat.seatCode }}</h3>
              <p>坐标：({{ seat.x }}, {{ seat.y }})</p>
            </div>
            <div class="seat-card-footer">
              <span :class="['seat-tag', { muted: seat.reserved || seat.seatStatus !== 1 }]">
                {{ seatStatusText(seat) }}
              </span>
              <button
                class="primary-button"
                :disabled="seat.reserved || seat.seatStatus !== 1 || reservingSeatId === seat.seatId"
                @click="reserveSeat(seat)"
              >
                {{ reservingSeatId === seat.seatId ? '预约中...' : '预约此座位' }}
              </button>
            </div>
          </article>
        </div>
      </section>
    </template>
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
.feedback {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

.room-meta {
  max-width: 320px;
}

.status {
  display: inline-flex;
  margin-bottom: 12px;
  background: #dcfce7;
  color: #166534;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 14px;
}

.status.inactive {
  background: #f3f4f6;
  color: #4b5563;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #374151;
  font-weight: 500;
}

input {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}

.actions {
  margin-top: 20px;
}

.primary-button {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  background: #2563eb;
  color: #fff;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.feedback {
  color: #374151;
}

.feedback.inline {
  margin-top: 16px;
  padding: 16px;
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

.section-header p,
.room-meta p,
.seat-card p {
  margin: 8px 0 0;
  color: #4b5563;
}

.seat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.seat-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 14px;
  padding: 20px;
}

.seat-card.reserved {
  border-color: #e5e7eb;
  background: #f9fafb;
}

.seat-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.seat-tag {
  color: #1d4ed8;
  font-weight: 600;
}

.seat-tag.muted {
  color: #6b7280;
}

@media (max-width: 768px) {
  .page-header,
  .seat-card-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
