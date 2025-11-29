<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import SeatMap from '@/components/SeatMap.vue'
import SeatDetailDrawer from '@/components/SeatDetailDrawer.vue'
import { getRoomMapConfig } from '@/config/roomMapConfig'
import { createSeatMapLayout, getSeatEnvironmentTags } from '@/utils/seatMapLayout'
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
const selectedSeat = ref(null)
const drawerVisible = ref(false)

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

const roomMapConfig = computed(() => getRoomMapConfig(roomId.value))
const roomLayout = computed(() => createSeatMapLayout(seatStatusList.value, roomMapConfig.value))

const selectedSeatEnvironment = computed(() => {
  if (!selectedSeat.value) {
    return []
  }

  const mappedSeat = roomLayout.value.seats.find((item) => item.seatId === selectedSeat.value.seatId)
  if (!mappedSeat) {
    return []
  }

  return getSeatEnvironmentTags(mappedSeat, roomLayout.value)
})

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

async function querySeatStatus(options = {}) {
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

    if (options.preserveSelectedSeat && selectedSeat.value) {
      selectedSeat.value = data.find((item) => item.seatId === selectedSeat.value.seatId) || null
      drawerVisible.value = Boolean(selectedSeat.value)
    } else {
      selectedSeat.value = null
      drawerVisible.value = false
    }
  } catch (error) {
    seatStatusList.value = []
    seatError.value = error.response?.data?.message || '查询座位状态失败，请稍后重试。'
  } finally {
    loadingSeats.value = false
  }
}

async function reserveSeat(seat = selectedSeat.value) {
  successMessage.value = ''
  seatError.value = ''

  if (!seat) {
    seatError.value = '请先在地图上选择一个座位。'
    return
  }

  if (!validateTimeRange()) {
    return
  }

  reservingSeatId.value = seat.seatId

  try {
    const { data } = await createReservation(buildPayload(seat.seatId))
    successMessage.value = `${seat.seatCode} 预约成功。${data.message || ''}`.trim()
    await querySeatStatus({ preserveSelectedSeat: true })
  } catch (error) {
    seatError.value = error.response?.data?.message || '创建预约失败，请稍后重试。'
  } finally {
    reservingSeatId.value = null
  }
}

function seatStatusText(seat) {
  if (!seat) {
    return '未选择'
  }

  if (seat.seatStatus !== 1) {
    return '不可用'
  }

  return seat.reserved ? '已被预约' : '空闲可预约'
}

function handleSelectSeat(seat) {
  selectedSeat.value = seat
  drawerVisible.value = true
  successMessage.value = ''
  seatError.value = ''
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
          <button class="primary-button" :disabled="loadingSeats || room.status !== 1" @click="querySeatStatus()">
            {{ loadingSeats ? '查询中...' : '查询该时段座位状态' }}
          </button>
        </div>
      </section>

      <div v-if="seatError" class="feedback error">{{ seatError }}</div>
      <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>

      <section class="panel">
        <div class="section-header">
          <div>
            <h2>2D 座位地图</h2>
            <p v-if="hasQueried">当前可预约座位：{{ availableSeats.length }} 个。点击任意座位可查看详情、留言并尝试预约。</p>
            <p v-else>请先选择时段并点击查询，随后在地图上点击任意座位查看详情。</p>
          </div>
        </div>

        <div v-if="loadingSeats" class="feedback inline">正在加载座位状态...</div>
        <div v-else-if="hasQueried && !seatStatusList.length" class="feedback inline">当前时段暂无座位数据。</div>
        <div v-else-if="hasQueried" class="map-layout">
          <SeatMap
            :room="room"
            :seat-status-list="seatStatusList"
            :map-config="roomMapConfig"
            :selected-seat-id="selectedSeat?.seatId || null"
            :reserving-seat-id="reservingSeatId"
            @select-seat="handleSelectSeat"
          />

          <aside class="selection-panel">
            <h3>选座概览</h3>
            <div v-if="selectedSeat" class="selection-card">
              <p><strong>座位号：</strong>{{ selectedSeat.seatCode }}</p>
              <p><strong>状态：</strong>{{ seatStatusText(selectedSeat) }}</p>
              <p><strong>坐标：</strong>({{ selectedSeat.x }}, {{ selectedSeat.y }})</p>
              <p>
                <strong>周边环境：</strong>
                {{ selectedSeatEnvironment.length ? selectedSeatEnvironment.join('、') : '普通区域' }}
              </p>
              <p>已为你打开详情面板，可在其中留言或完成预约。</p>
              <button class="primary-button full-width" @click="drawerVisible = true">打开/返回详情窗口</button>
            </div>
            <div v-else class="selection-empty">
              请从左侧地图点击任意座位，系统会在详情窗口中展示座位号、状态、坐标、周边环境和留言板。
            </div>

            <div class="tips-card">
              <h4>地图提示</h4>
              <ul>
                <li>优先观察门口、窗户和座位排布，判断座位的整体分区。</li>
                <li>蓝色表示空闲可预约，红色表示已预约，灰色表示当前不可预约。</li>
                <li>点击任意座位都能打开详情窗口，查看信息并留言。</li>
                <li>已预约或不可预约的座位也支持打开详情窗口并留言。</li>
              </ul>
            </div>
          </aside>
        </div>
      </section>

      <SeatDetailDrawer
        :visible="drawerVisible"
        :seat="selectedSeat"
        :environment-tags="selectedSeatEnvironment"
        :loading-seat-status="loadingSeats"
        :reserving-seat-id="reservingSeatId"
        @close="drawerVisible = false"
        @confirm-reserve="reserveSeat"
      />
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

.full-width {
  width: 100%;
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
.room-meta p {
  margin: 8px 0 0;
  color: #4b5563;
}

.map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.9fr);
  gap: 20px;
  margin-top: 20px;
  align-items: start;
}

.selection-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.selection-panel h3,
.tips-card h4 {
  margin: 0;
}

.selection-card,
.selection-empty,
.tips-card {
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 14px;
  padding: 18px;
}

.selection-card p,
.selection-empty,
.tips-card li {
  color: #475569;
}

.tips-card ul {
  margin: 12px 0 0;
  padding-left: 18px;
}

@media (max-width: 960px) {
  .map-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
