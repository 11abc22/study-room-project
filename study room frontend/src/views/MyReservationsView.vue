<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  cancelReservation,
  getMyReservations,
  getRoomSeatStatus,
  getRooms,
  updateReservation
} from '@/services/reservationApi'
import {
  approveSwapRequest,
  decorateMyReservations,
  getMyPendingSwapRequest,
  rejectSwapRequest,
  withdrawSwapRequest
} from '@/services/swapRequestApi'
import SwapRequestManagementModal from '@/components/SwapRequestManagementModal.vue'
import {
  getReservationStatusMeta,
  isPendingStatus,
  isRequestingStatus,
  isReservedStatus
} from '@/constants/reservationStatus'

const reservations = ref([])
const rooms = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const cancellingId = ref(null)
const savingId = ref(null)
const withdrawingId = ref(null)

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

const swapManagementVisible = ref(false)
const loadingSwapDetail = ref(false)
const swapActionLoading = ref(false)
const managedReservation = ref(null)
const pendingSwapRequest = ref(null)

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

const managedReservationTitle = computed(() => {
  if (!managedReservation.value) {
    return '--'
  }

  return `${managedReservation.value.roomName} · ${managedReservation.value.seatCode}`
})

const managedReservationSchedule = computed(() => {
  if (!managedReservation.value) {
    return '--'
  }

  return formatSchedule(managedReservation.value)
})

async function loadBaseData() {
  loading.value = true
  errorMessage.value = ''

  try {
    const [{ data: roomData }, { data: reservationData }] = await Promise.all([getRooms(), getMyReservations()])
    rooms.value = roomData
    reservations.value = decorateMyReservations(reservationData)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to load reservation data. Please try again later.'
  } finally {
    loading.value = false
  }
}

function formatSchedule(reservation) {
  return `${reservation.reserveDate} ${String(reservation.startTime).slice(0, 5)} - ${String(reservation.endTime).slice(0, 5)}`
}

function reservationStatusMeta(status) {
  return getReservationStatusMeta(status)
}

function canEditReservation(reservation) {
  return isReservedStatus(reservation.status) && !reservation.isVirtualSwapRecord
}

function validateEditForm() {
  if (
    !editForm.value.roomId ||
    !editForm.value.seatId ||
    !editForm.value.reserveDate ||
    !editForm.value.startTime ||
    !editForm.value.endTime
  ) {
    editErrorMessage.value = 'Please complete the room, seat, date, and time fields.'
    return false
  }

  if (editForm.value.startTime >= editForm.value.endTime) {
    editErrorMessage.value = 'Start time must be earlier than end time.'
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
    editErrorMessage.value = 'Start time must be earlier than end time.'
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
    editErrorMessage.value = error.response?.data?.message || 'Failed to load editable seat availability. Please try again later.'
  } finally {
    loadingEditSeats.value = false
  }
}

function startEdit(reservation) {
  if (!canEditReservation(reservation)) {
    return
  }

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

    successMessage.value = data.message || 'Reservation updated successfully.'
    cancelEdit()
    await loadBaseData()
  } catch (error) {
    editErrorMessage.value = error.response?.data?.message || 'Failed to update the reservation. Please try again later.'
  } finally {
    savingId.value = null
  }
}

async function handleCancelReservation(reservation) {
  if (!canEditReservation(reservation)) {
    return
  }

  successMessage.value = ''
  errorMessage.value = ''
  cancellingId.value = reservation.id

  try {
    const { data } = await cancelReservation(reservation.id)
    successMessage.value = data.message || 'Reservation cancelled.'
    if (editingReservationId.value === reservation.id) {
      cancelEdit()
    }
    await loadBaseData()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to cancel the reservation. Please try again later.'
  } finally {
    cancellingId.value = null
  }
}

async function handleWithdrawSwapRequest(reservation) {
  if (!reservation?.swapRequestId || withdrawingId.value === reservation.id) {
    return
  }

  if (!window.confirm('Confirm withdrawing this seat swap request?')) {
    return
  }

  successMessage.value = ''
  errorMessage.value = ''
  withdrawingId.value = reservation.id

  try {
    const { data } = await withdrawSwapRequest(reservation.swapRequestId)
    successMessage.value = data.message || 'Seat swap request withdrawn.'
    await loadBaseData()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'Failed to withdraw the seat swap request.'
  } finally {
    withdrawingId.value = null
  }
}

function closeSwapManagement() {
  swapManagementVisible.value = false
  managedReservation.value = null
  pendingSwapRequest.value = null
}

async function openSwapManagement(reservation) {
  managedReservation.value = reservation
  pendingSwapRequest.value = null
  swapManagementVisible.value = true
  loadingSwapDetail.value = true
  successMessage.value = ''
  errorMessage.value = ''

  try {
    const { data } = await getMyPendingSwapRequest({
      reservationId: reservation.id,
      roomId: reservation.roomId,
      seatId: reservation.seatId,
      reserveDate: reservation.reserveDate,
      startTime: reservation.startTime,
      endTime: reservation.endTime
    })

    pendingSwapRequest.value = data.data || data || null
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to load the swap request details.'
  } finally {
    loadingSwapDetail.value = false
  }
}

async function handleApproveSwapRequest() {
  if (!pendingSwapRequest.value?.swapRequestId || swapActionLoading.value) {
    return
  }

  if (!window.confirm('Approving this request will cancel your current reservation. Continue?')) {
    return
  }

  swapActionLoading.value = true
  successMessage.value = ''
  errorMessage.value = ''

  try {
    const { data } = await approveSwapRequest(pendingSwapRequest.value.swapRequestId)
    successMessage.value = data.message || 'Seat swap request approved.'
    closeSwapManagement()
    await loadBaseData()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'Failed to approve the seat swap request.'
  } finally {
    swapActionLoading.value = false
  }
}

async function handleRejectSwapRequest() {
  if (!pendingSwapRequest.value?.swapRequestId || swapActionLoading.value) {
    return
  }

  if (!window.confirm('Confirm rejecting this seat swap request?')) {
    return
  }

  swapActionLoading.value = true
  successMessage.value = ''
  errorMessage.value = ''

  try {
    const { data } = await rejectSwapRequest(pendingSwapRequest.value.swapRequestId)
    successMessage.value = data.message || 'Seat swap request rejected.'
    closeSwapManagement()
    await loadBaseData()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'Failed to reject the seat swap request.'
  } finally {
    swapActionLoading.value = false
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
        <p class="eyebrow">Reservation Center</p>
        <h1>My Reservations</h1>
        <p>Review your reservations, manage seat swap requests, cancel bookings, or edit an existing reservation.</p>
      </div>
    </header>

    <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>
    <div v-if="loading" class="feedback">Loading your reservations...</div>
    <div v-else-if="!reservations.length" class="feedback">You do not have any reservations yet.</div>

    <div v-else class="reservation-list">
      <article v-for="reservation in reservations" :key="reservation.id" class="reservation-card">
        <div class="reservation-header">
          <div>
            <h2>{{ reservation.roomName }} · {{ reservation.seatCode }}</h2>
            <p>{{ formatSchedule(reservation) }}</p>
          </div>
          <span :class="['status', reservationStatusMeta(reservation.status).className]">
            {{ reservationStatusMeta(reservation.status).label }}
          </span>
        </div>

        <div v-if="editingReservationId === reservation.id" class="edit-panel">
          <div class="form-grid">
            <label>
              <span>Room</span>
              <select v-model="editForm.roomId" @change="loadEditSeatStatus">
                <option value="">Select a room</option>
                <option v-for="room in roomOptions" :key="room.id" :value="String(room.id)">
                  {{ room.name }}
                </option>
              </select>
            </label>

            <label>
              <span>Date</span>
              <input v-model="editForm.reserveDate" type="date" @change="loadEditSeatStatus" />
            </label>

            <label>
              <span>Start Time</span>
              <input v-model="editForm.startTime" type="time" @change="loadEditSeatStatus" />
            </label>

            <label>
              <span>End Time</span>
              <input v-model="editForm.endTime" type="time" @change="loadEditSeatStatus" />
            </label>

            <label>
              <span>Seat</span>
              <select v-model="editForm.seatId">
                <option value="">Select a seat</option>
                <option v-for="seat in editableAvailableSeats" :key="seat.seatId" :value="String(seat.seatId)">
                  {{ seat.seatCode }}
                </option>
              </select>
            </label>
          </div>

          <div v-if="loadingEditSeats" class="inline-tip">Checking available seats for this time slot...</div>
          <div v-else-if="editableAvailableSeats.length" class="inline-tip success-text">
            Available seats: {{ editableAvailableSeats.map((seat) => seat.seatCode).join(', ') }}
          </div>
          <div v-else class="inline-tip">No seats are available for the selected conditions.</div>

          <div v-if="editErrorMessage" class="inline-tip error-text">{{ editErrorMessage }}</div>

          <div class="actions">
            <button class="primary-button" :disabled="savingId === reservation.id" @click="submitEdit(reservation.id)">
              {{ savingId === reservation.id ? 'Saving...' : 'Save Changes' }}
            </button>
            <button class="ghost-button" @click="cancelEdit">Discard</button>
          </div>
        </div>

        <div v-else class="actions actions-wrap">
          <template v-if="canEditReservation(reservation)">
            <button class="primary-button" :disabled="savingId === reservation.id" @click="startEdit(reservation)">
              Edit Reservation
            </button>
            <button
              class="danger-button"
              :disabled="cancellingId === reservation.id"
              @click="handleCancelReservation(reservation)"
            >
              {{ cancellingId === reservation.id ? 'Cancelling...' : 'Cancel Reservation' }}
            </button>
          </template>

          <template v-else-if="isRequestingStatus(reservation.status)">
            <button
              class="warning-button"
              :disabled="withdrawingId === reservation.id"
              @click="handleWithdrawSwapRequest(reservation)"
            >
              {{ withdrawingId === reservation.id ? 'Withdrawing...' : 'Withdraw Request' }}
            </button>
          </template>

          <template v-else-if="isPendingStatus(reservation.status)">
            <button class="primary-button" @click="openSwapManagement(reservation)">View Swap Request</button>
          </template>

          <p v-else-if="reservation.isVirtualSwapRecord" class="inline-tip success-text no-margin">
            This record is currently provided by the front-end mock workflow.
          </p>
        </div>
      </article>
    </div>

    <SwapRequestManagementModal
      :visible="swapManagementVisible"
      :reservation-title="managedReservationTitle"
      :schedule-text="managedReservationSchedule"
      :request-data="pendingSwapRequest"
      :loading="loadingSwapDetail"
      :acting="swapActionLoading"
      @close="closeSwapManagement"
      @approve="handleApproveSwapRequest"
      @reject="handleRejectSwapRequest"
    />
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
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
}

.status.reserved {
  background: #dcfce7;
  color: #166534;
}

.status.cancelled {
  background: #f3f4f6;
  color: #4b5563;
}

.status.requesting {
  background: #fef3c7;
  color: #b45309;
}

.status.pending {
  background: #dbeafe;
  color: #1d4ed8;
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

.actions-wrap {
  flex-wrap: wrap;
  align-items: center;
}

.primary-button,
.ghost-button,
.danger-button,
.warning-button {
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

.warning-button {
  background: #fef3c7;
  color: #b45309;
}

.primary-button:disabled,
.danger-button:disabled,
.warning-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.inline-tip {
  margin-top: 16px;
  color: #4b5563;
}

.no-margin {
  margin-top: 0;
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
