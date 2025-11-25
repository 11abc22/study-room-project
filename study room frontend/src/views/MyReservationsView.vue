<script setup>
import { computed, onMounted, ref } from 'vue'
import { cancelReservation, getMyReservations, getRoomSeatStatus, getRooms, updateReservation } from '@/services/reservationApi'

const reservations = ref([])
const rooms = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const cancellingId = ref(null)
const savingId = ref(null)

const editingReservationId = ref(null)
const editForm = ref({
  roomId: '',
  seatId: '',
  reserveDate: '',
  startTime: '',
  endTime: ''
})
const editSeatStatusList = ref([])
const loadingEditSeats = ref(false)
const editErrorMessage = ref('')

const roomOptions = computed(() => rooms.value)
const editableAvailableSeats = computed(() => {
  if (!editingReservationId.value) {
    return []
  }

  const currentReservation = reservations.value.find((item) => item.id === editingReservationId.value)

  return editSeatStatusList.value.filter(
    (seat) => seat.seatStatus === 1 && (!seat.reserved || seat.seatId === currentReservation?.seatId)
  )
})

async function loadBaseData() {
  loading.value = true
  errorMessage.value = ''

  try {
    const [{ data: roomData }, { data: reservationData }] = await Promise.all([getRooms(), getMyReservations()])
    rooms.value = roomData
    reservations.value = reservationData
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '加载预约信息失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function formatSchedule(reservation) {
  return `${reservation.reserveDate} ${String(reservation.startTime).slice(0, 5)} - ${String(reservation.endTime).slice(0, 5)}`
}

function reservationStatusText(status) {
  if (status === 1) {
    return '已预约'
  }

  if (status === 2) {
    return '已取消'
  }

  return '未知状态'
}

function validateEditForm() {
  if (
    !editForm.value.roomId ||
    !editForm.value.seatId ||
    !editForm.value.reserveDate ||
    !editForm.value.startTime ||
    !editForm.value.endTime
  ) {
    editErrorMessage.value = '请完整填写房间、座位、日期和时间。'
    return false
  }

  if (editForm.value.startTime >= editForm.value.endTime) {
    editErrorMessage.value = '开始时间必须早于结束时间。'
    return false
  }

  return true
}

async function loadEditSeatStatus() {
  editErrorMessage.value = ''
  editSeatStatusList.value = []

  if (!editForm.value.roomId || !editForm.value.reserveDate || !editForm.value.startTime || !editForm.value.endTime) {
    return
  }

  if (editForm.value.startTime >= editForm.value.endTime) {
    editErrorMessage.value = '开始时间必须早于结束时间。'
    return
  }

  loadingEditSeats.value = true

  try {
    const { data } = await getRoomSeatStatus(editForm.value.roomId, {
      roomId: Number(editForm.value.roomId),
      reserveDate: editForm.value.reserveDate,
      startTime: editForm.value.startTime,
      endTime: editForm.value.endTime
    })

    editSeatStatusList.value = data

    const currentReservation = reservations.value.find((item) => item.id === editingReservationId.value)
    const selectedSeatAvailable = data.some(
      (seat) => seat.seatId === Number(editForm.value.seatId) && (seat.seatStatus === 1 && (!seat.reserved || seat.seatId === currentReservation?.seatId))
    )

    if (!selectedSeatAvailable) {
      const fallbackSeat = data.find(
        (seat) => seat.seatStatus === 1 && (!seat.reserved || seat.seatId === currentReservation?.seatId)
      )
      editForm.value.seatId = fallbackSeat ? String(fallbackSeat.seatId) : ''
    }
  } catch (error) {
    editErrorMessage.value = error.response?.data?.message || '查询可编辑座位状态失败，请稍后重试。'
  } finally {
    loadingEditSeats.value = false
  }
}

function startEdit(reservation) {
  successMessage.value = ''
  errorMessage.value = ''
  editingReservationId.value = reservation.id
  editForm.value = {
    roomId: String(reservation.roomId),
    seatId: String(reservation.seatId),
    reserveDate: reservation.reserveDate,
    startTime: String(reservation.startTime).slice(0, 5),
    endTime: String(reservation.endTime).slice(0, 5)
  }
  loadEditSeatStatus()
}

function cancelEdit() {
  editingReservationId.value = null
  editSeatStatusList.value = []
  editErrorMessage.value = ''
}

async function submitEdit(reservationId) {
  successMessage.value = ''
  errorMessage.value = ''
  editErrorMessage.value = ''

  if (!validateEditForm()) {
    return
  }

  savingId.value = reservationId

  try {
    const { data } = await updateReservation(reservationId, {
      roomId: Number(editForm.value.roomId),
      seatId: Number(editForm.value.seatId),
      reserveDate: editForm.value.reserveDate,
      startTime: editForm.value.startTime,
      endTime: editForm.value.endTime
    })

    successMessage.value = data.message || '预约修改成功。'
    cancelEdit()
    await loadBaseData()
  } catch (error) {
    editErrorMessage.value = error.response?.data?.message || '修改预约失败，请稍后重试。'
  } finally {
    savingId.value = null
  }
}

async function handleCancelReservation(reservationId) {
  successMessage.value = ''
  errorMessage.value = ''
  cancellingId.value = reservationId

  try {
    const { data } = await cancelReservation(reservationId)
    successMessage.value = data.message || '预约已取消。'
    if (editingReservationId.value === reservationId) {
      cancelEdit()
    }
    await loadBaseData()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '取消预约失败，请稍后重试。'
  } finally {
    cancellingId.value = null
  }
}

onMounted(() => {
  loadBaseData()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">预约中心</p>
        <h1>我的预约</h1>
        <p>查看自己已创建的预约，支持取消预约，以及直接编辑并提交更新。</p>
      </div>
    </header>

    <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>
    <div v-if="loading" class="feedback">正在加载我的预约...</div>
    <div v-else-if="!reservations.length" class="feedback">当前还没有预约记录。</div>

    <div v-else class="reservation-list">
      <article v-for="reservation in reservations" :key="reservation.id" class="reservation-card">
        <div class="reservation-header">
          <div>
            <h2>{{ reservation.roomName }} · {{ reservation.seatCode }}</h2>
            <p>{{ formatSchedule(reservation) }}</p>
          </div>
          <span :class="['status', { cancelled: reservation.status === 2 }]">
            {{ reservationStatusText(reservation.status) }}
          </span>
        </div>

        <div v-if="editingReservationId === reservation.id" class="edit-panel">
          <div class="form-grid">
            <label>
              <span>房间</span>
              <select v-model="editForm.roomId" @change="loadEditSeatStatus">
                <option value="">请选择房间</option>
                <option v-for="room in roomOptions" :key="room.id" :value="String(room.id)">
                  {{ room.name }}
                </option>
              </select>
            </label>

            <label>
              <span>日期</span>
              <input v-model="editForm.reserveDate" type="date" @change="loadEditSeatStatus" />
            </label>

            <label>
              <span>开始时间</span>
              <input v-model="editForm.startTime" type="time" @change="loadEditSeatStatus" />
            </label>

            <label>
              <span>结束时间</span>
              <input v-model="editForm.endTime" type="time" @change="loadEditSeatStatus" />
            </label>

            <label>
              <span>座位</span>
              <select v-model="editForm.seatId">
                <option value="">请选择座位</option>
                <option v-for="seat in editableAvailableSeats" :key="seat.seatId" :value="String(seat.seatId)">
                  {{ seat.seatCode }}
                </option>
              </select>
            </label>
          </div>

          <div v-if="loadingEditSeats" class="inline-tip">正在查询该时段可选座位...</div>
          <div v-else-if="editableAvailableSeats.length" class="inline-tip success-text">
            当前可选座位：{{ editableAvailableSeats.map((seat) => seat.seatCode).join('、') }}
          </div>
          <div v-else class="inline-tip">当前条件下暂无可选座位。</div>

          <div v-if="editErrorMessage" class="inline-tip error-text">{{ editErrorMessage }}</div>

          <div class="actions">
            <button class="primary-button" :disabled="savingId === reservation.id" @click="submitEdit(reservation.id)">
              {{ savingId === reservation.id ? '提交中...' : '提交修改' }}
            </button>
            <button class="ghost-button" @click="cancelEdit">取消编辑</button>
          </div>
        </div>

        <div v-else class="actions">
          <button class="primary-button" :disabled="reservation.status !== 1" @click="startEdit(reservation)">
            编辑预约
          </button>
          <button
            class="danger-button"
            :disabled="reservation.status !== 1 || cancellingId === reservation.id"
            @click="handleCancelReservation(reservation.id)"
          >
            {{ cancellingId === reservation.id ? '取消中...' : '取消预约' }}
          </button>
        </div>
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

.page-header,
.feedback,
.reservation-card {
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

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reservation-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.reservation-header h2 {
  margin: 0 0 8px;
}

.reservation-header p {
  margin: 0;
  color: #4b5563;
}

.status {
  background: #dcfce7;
  color: #166534;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 14px;
}

.status.cancelled {
  background: #f3f4f6;
  color: #4b5563;
}

.edit-panel {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.form-grid {
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
}

input,
select {
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

.primary-button:disabled,
.danger-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.inline-tip {
  margin-top: 16px;
  color: #4b5563;
}

.error-text {
  color: #b91c1c;
}

.success-text {
  color: #166534;
}

@media (max-width: 768px) {
  .reservation-header,
  .actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
