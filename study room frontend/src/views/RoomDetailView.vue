<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import RoomTimeline from '@/components/RoomTimeline.vue'
import SeatMap from '@/components/SeatMap.vue'
import SeatDetailDrawer from '@/components/SeatDetailDrawer.vue'
import SwapRequestMessageModal from '@/components/SwapRequestMessageModal.vue'
import { getRoomMapConfig } from '@/config/roomMapConfig'
import { createSeatMapLayout, getSeatEnvironmentTags } from '@/utils/seatMapLayout'
import { createReservation, getRoomSeatStatus, getRoomTimeline, getRooms } from '@/services/reservationApi'
import { checkSwapRequestAvailability, createSwapRequest } from '@/services/swapRequestApi'

const route = useRoute()
const authStore = useAuthStore()
const roomId = computed(() => Number(route.params.id))

const room = ref(null)
const loadingRoom = ref(false)
const loadingSeats = ref(false)
const loadingRoomTimeline = ref(false)
const reservingSeatId = ref(null)
const roomError = ref('')
const seatError = ref('')
const roomTimelineError = ref('')
const successMessage = ref('')
const selectedSeat = ref(null)
const drawerVisible = ref(false)
const roomTimeline = ref([])
const currentSeatTimeline = ref([])
const timelineSelectionSource = ref('room')
const swapAvailability = ref({
  canRequest: false,
  reason: 'CAN_REQUEST'
})
const checkingSwapAvailability = ref(false)
const swapModalVisible = ref(false)
const swapSubmitting = ref(false)
let seatStatusRefreshTimer = null

const timeOptions = Array.from({ length: 16 }, (_, index) => {
  const hour = index + 8
  return {
    label: `${String(hour).padStart(2, '0')}:00`,
    value: `${String(hour).padStart(2, '0')}:00`
  }
})

const form = ref({
  reserveDate: '',
  startTime: '08:00',
  endTime: '09:00'
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

const selectedSeatLabel = computed(() => {
  if (!selectedSeat.value) {
    return '--'
  }

  return `${room.value?.name || 'Study Room'} - ${selectedSeat.value.seatCode}`
})

const selectedScheduleText = computed(() => {
  if (!form.value.reserveDate || !form.value.startTime || !form.value.endTime) {
    return '--'
  }

  return `${form.value.reserveDate} ${form.value.startTime} - ${form.value.endTime}`
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

function buildSwapTargetPayload(seat = selectedSeat.value) {
  if (!seat) {
    return null
  }

  return {
    roomId: roomId.value,
    roomName: room.value?.name || 'Study Room',
    seatId: seat.seatId,
    seatCode: seat.seatCode,
    reserveDate: form.value.reserveDate,
    startTime: form.value.startTime,
    endTime: form.value.endTime,
    reservationId: seat.reservationId || seat.targetReservationId || null,
    targetReservationId: seat.reservationId || seat.targetReservationId || null,
    holderUserId: seat.userId || seat.reservedUserId || seat.holderUserId || null,
    holderName: seat.username || seat.reservedUsername || seat.holderName || ''
  }
}

function validateTimeRange() {
  if (!form.value.reserveDate || !form.value.startTime || !form.value.endTime) {
    seatError.value = 'Please select the reservation date, start time, and end time.'
    return false
  }

  if (form.value.startTime >= form.value.endTime) {
    seatError.value = 'Start time must be earlier than end time.'
    return false
  }

  return true
}

function toTimeString(hour) {
  return `${String(hour).padStart(2, '0')}:00`
}

function toHour(value) {
  if (!value) {
    return null
  }

  const [hour] = value.split(':')
  return Number(hour)
}

function applySuggestedHour(hour) {
  updateTimeRange({
    startTime: toTimeString(hour),
    endTime: toTimeString(hour + 1)
  })
}

function findSuggestedHour(timeline = []) {
  const availableHour = timeline.find((item) => {
    if (!item) {
      return false
    }

    return item.status !== 'busy' && item.available > 0
  })

  return availableHour?.hour ?? 8
}

function updateTimeRange({ startTime, endTime }) {
  if (startTime) {
    form.value.startTime = startTime
  }

  if (endTime) {
    form.value.endTime = endTime
  }
}

function isRoomHourSelectable(item) {
  return Boolean(item) && item.status !== 'busy' && item.available > 0
}

function isSeatHourSelectable(item) {
  return Boolean(item) && item.available !== false
}

function canExtendSelection(timeline, startHour, clickedHour, isSelectable) {
  const startIndex = timeline.findIndex((item) => item.hour === startHour)
  const endIndex = timeline.findIndex((item) => item.hour === clickedHour)

  if (startIndex === -1 || endIndex === -1 || endIndex < startIndex) {
    return false
  }

  return timeline.slice(startIndex, endIndex + 1).every((item) => isSelectable(item))
}

function setTimelineSelection(hour) {
  updateTimeRange({
    startTime: toTimeString(hour),
    endTime: toTimeString(hour + 1)
  })
}

function handleTimelineHourClick(hour, source) {
  const timeline = source === 'seat' ? currentSeatTimeline.value : roomTimeline.value
  const isSelectable = source === 'seat' ? isSeatHourSelectable : isRoomHourSelectable
  const clickedItem = timeline.find((item) => item.hour === hour)

  if (!isSelectable(clickedItem)) {
    return
  }

  const currentStartHour = toHour(form.value.startTime)
  const currentEndHour = toHour(form.value.endTime)
  const isCurrentSelectionOnSameSource = timelineSelectionSource.value === source

  if (!isCurrentSelectionOnSameSource || currentStartHour === null || currentEndHour === null || currentEndHour - currentStartHour !== 1) {
    timelineSelectionSource.value = source
    setTimelineSelection(hour)
    return
  }

  if (hour <= currentStartHour) {
    timelineSelectionSource.value = source
    setTimelineSelection(hour)
    return
  }

  if (canExtendSelection(timeline, currentStartHour, hour, isSelectable)) {
    timelineSelectionSource.value = source
    updateTimeRange({
      startTime: toTimeString(currentStartHour),
      endTime: toTimeString(hour + 1)
    })
    return
  }

  timelineSelectionSource.value = source
  setTimelineSelection(hour)
}

async function loadRoom() {
  loadingRoom.value = true
  roomError.value = ''

  try {
    const { data } = await getRooms()
    room.value = data.find((item) => item.id === roomId.value) || null

    if (!room.value) {
      roomError.value = 'Study room not found.'
    }
  } catch (error) {
    roomError.value = error.response?.data?.message || 'Failed to load room details. Please try again later.'
  } finally {
    loadingRoom.value = false
  }
}

async function loadRoomTimeline() {
  roomTimeline.value = []
  roomTimelineError.value = ''

  if (!form.value.reserveDate) {
    return []
  }

  loadingRoomTimeline.value = true

  try {
    const { data } = await getRoomTimeline(roomId.value, form.value.reserveDate)
    roomTimeline.value = data
    return data
  } catch (error) {
    roomTimelineError.value = error.response?.data?.message || 'Failed to load room timeline. Please try again later.'
    return []
  } finally {
    loadingRoomTimeline.value = false
  }
}

async function querySeatStatus(options = {}) {
  if (!options.silent) {
    successMessage.value = ''
    seatError.value = ''
  }

  if (!validateTimeRange()) {
    return
  }

  loadingSeats.value = true
  hasQueried.value = true

  try {
    const { data } = await getRoomSeatStatus(roomId.value, buildPayload())
    seatStatusList.value = data

    if (selectedSeat.value) {
      const nextSelectedSeat = data.find((item) => item.seatId === selectedSeat.value.seatId) || null
      selectedSeat.value = nextSelectedSeat
      drawerVisible.value = options.preserveSelectedSeat ? Boolean(nextSelectedSeat) : drawerVisible.value
    } else if (!options.keepSelection) {
      drawerVisible.value = false
    }
  } catch (error) {
    seatStatusList.value = []
    seatError.value = error.response?.data?.message || 'Failed to load seat status. Please try again later.'
  } finally {
    loadingSeats.value = false
  }
}

async function reserveSeat(seat = selectedSeat.value) {
  successMessage.value = ''
  seatError.value = ''

  if (!seat) {
    seatError.value = 'Please select a seat on the map first.'
    return
  }

  if (!validateTimeRange()) {
    return
  }

  reservingSeatId.value = seat.seatId

  try {
    const { data } = await createReservation(buildPayload(seat.seatId))
    successMessage.value = `${seat.seatCode} reserved successfully. ${data.message || ''}`.trim()
    await loadRoomTimeline()
    await querySeatStatus({ preserveSelectedSeat: true })
    await loadSwapAvailability()
  } catch (error) {
    seatError.value = error.response?.data?.message || 'Failed to create the reservation. Please try again later.'
  } finally {
    reservingSeatId.value = null
  }
}

function resetSwapAvailability() {
  swapAvailability.value = {
    canRequest: false,
    reason: 'CAN_REQUEST'
  }
}

async function loadSwapAvailability() {
  if (!drawerVisible.value || !selectedSeat.value || !form.value.reserveDate || !form.value.startTime || !form.value.endTime) {
    resetSwapAvailability()
    return
  }

  if (selectedSeat.value.seatStatus !== 1 || !selectedSeat.value.reserved) {
    resetSwapAvailability()
    return
  }

  checkingSwapAvailability.value = true

  try {
    const target = buildSwapTargetPayload(selectedSeat.value)
    const { data } = await checkSwapRequestAvailability({
      reservationId: target?.targetReservationId ?? target?.reservationId ?? undefined,
      roomId: target?.roomId,
      seatId: target?.seatId,
      reserveDate: target?.reserveDate,
      startTime: target?.startTime,
      endTime: target?.endTime,
      holderUserId: target?.holderUserId
    })

    swapAvailability.value = data.data || data
  } catch (error) {
    resetSwapAvailability()
    seatError.value = error.response?.data?.message || 'Failed to check swap availability. Please try again later.'
  } finally {
    checkingSwapAvailability.value = false
  }
}

function openSwapModal() {
  if (!selectedSeat.value || checkingSwapAvailability.value || !swapAvailability.value.canRequest) {
    return
  }

  swapModalVisible.value = true
}

async function submitSwapRequest(message) {
  const target = buildSwapTargetPayload(selectedSeat.value)

  if (!target) {
    return
  }

  swapSubmitting.value = true
  seatError.value = ''
  successMessage.value = ''

  try {
    await createSwapRequest({
      targetReservationId: target.targetReservationId,
      message,
      requesterName: authStore.user?.username || authStore.user?.name || authStore.user?.email,
      target
    })
    swapModalVisible.value = false
    successMessage.value = `Swap request for ${target.seatCode} has been sent.`
    await loadSwapAvailability()
  } catch (error) {
    const businessMessage = error.code === 'ALREADY_REJECTED'
      ? 'You already requested this seat before.'
      : error.code === 'HAS_ACTIVE_REQUEST'
        ? 'You already have an active swap request in this time slot.'
        : error.code === 'SEAT_LOCKED'
          ? 'Another user is already requesting this seat.'
          : error.message

    seatError.value = error.response?.data?.message || businessMessage || 'Failed to send the swap request. Please try again later.'
  } finally {
    swapSubmitting.value = false
  }
}

function seatStatusText(seat) {
  if (!seat) {
    return 'No seat selected'
  }

  if (seat.seatStatus !== 1) {
    return 'Unavailable'
  }

  return seat.reserved ? 'Reserved' : 'Available'
}

function handleSelectSeat(seat) {
  selectedSeat.value = seat
  currentSeatTimeline.value = []
  drawerVisible.value = true
  successMessage.value = ''
  seatError.value = ''
}

function closeSeatDrawer() {
  drawerVisible.value = false
  swapModalVisible.value = false
  selectedSeat.value = null
  currentSeatTimeline.value = []
  resetSwapAvailability()
}

function handleSeatTimelineLoaded(timeline) {
  currentSeatTimeline.value = Array.isArray(timeline) ? timeline : []
}

function scheduleSeatStatusRefresh() {
  if (seatStatusRefreshTimer) {
    clearTimeout(seatStatusRefreshTimer)
  }

  seatStatusRefreshTimer = setTimeout(async () => {
    seatStatusRefreshTimer = null
    await querySeatStatus({
      preserveSelectedSeat: drawerVisible.value && Boolean(selectedSeat.value),
      keepSelection: drawerVisible.value && Boolean(selectedSeat.value),
      silent: true
    })
  }, 180)
}

watch(
  () => form.value.reserveDate,
  async (reserveDate) => {
    seatStatusList.value = []
    hasQueried.value = false
    selectedSeat.value = null
    currentSeatTimeline.value = []
    drawerVisible.value = false

    if (!reserveDate) {
      roomTimeline.value = []
      return
    }

    const timeline = await loadRoomTimeline()
    const suggestedHour = findSuggestedHour(timeline)
    applySuggestedHour(suggestedHour)

    if (!timeline.some((item) => item.status !== 'busy' && item.available > 0)) {
      seatError.value = 'The room is full for every hourly slot on the selected date. Showing 08:00-09:00 as the default query range.'
    } else {
      seatError.value = ''
    }

    await querySeatStatus({ keepSelection: false })
  }
)

watch(
  () => [form.value.startTime, form.value.endTime],
  async ([startTime, endTime], [previousStartTime, previousEndTime]) => {
    if (!form.value.reserveDate || !startTime || !endTime) {
      return
    }

    if (startTime === previousStartTime && endTime === previousEndTime) {
      return
    }

    if (!validateTimeRange()) {
      return
    }

    scheduleSeatStatusRefresh()
  }
)

watch(
  () => drawerVisible.value,
  (visible) => {
    if (!visible) {
      currentSeatTimeline.value = []
      swapModalVisible.value = false
      resetSwapAvailability()
    }
  }
)

watch(
  () => [drawerVisible.value, selectedSeat.value?.seatId, selectedSeat.value?.reserved, form.value.reserveDate, form.value.startTime, form.value.endTime],
  async ([visible]) => {
    if (!visible) {
      return
    }

    await loadSwapAvailability()
  }
)

onMounted(() => {
  loadRoom()
})
</script>

<template>
  <section class="page">
    <div v-if="loadingRoom" class="feedback">Loading room details...</div>
    <div v-else-if="roomError" class="feedback error">{{ roomError }}</div>

    <template v-else-if="room">
      <header class="page-header">
        <div>
          <p class="eyebrow">Room Details</p>
          <h1>{{ room.name }}</h1>
          <p>{{ room.location || 'Location not available' }}</p>
        </div>
        <div class="room-meta">
          <span :class="['status', { inactive: room.status !== 1 }]">
            {{ room.status === 1 ? 'Open' : 'Closed' }}
          </span>
          <p>{{ room.description || 'No room description available.' }}</p>
        </div>
      </header>

      <section class="panel">
        <div class="panel-heading">
          <div>
            <h2>Select a Time Slot</h2>
            <p>After you choose a date, the system automatically opens the seat list and suggests the first available one-hour slot from 08:00 onward.</p>
          </div>
        </div>
        <div class="form-grid compact-form-grid">
          <label>
            <span>Date</span>
            <input v-model="form.reserveDate" type="date" />
          </label>
          <label>
            <span>Start Time</span>
            <select v-model="form.startTime">
              <option v-for="option in timeOptions.slice(0, -1)" :key="`room-start-${option.value}`" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            <span>End Time</span>
            <select v-model="form.endTime">
              <option v-for="option in timeOptions.slice(1)" :key="`room-end-${option.value}`" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
        </div>
        <RoomTimeline
          :timeline="roomTimeline"
          :loading="loadingRoomTimeline"
          :error="roomTimelineError"
          :selected-start-hour="toHour(form.startTime)"
          :selected-end-hour="toHour(form.endTime)"
          @hour-click="(hour) => handleTimelineHourClick(hour, 'room')"
        />
        <div class="actions">
          <button class="primary-button" :disabled="loadingSeats || room.status !== 1" @click="querySeatStatus()">
            {{ loadingSeats ? 'Refreshing...' : 'Refresh Seat Availability' }}
          </button>
        </div>
      </section>

      <div v-if="seatError" class="feedback error">{{ seatError }}</div>
      <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>

      <section class="panel">
        <div class="section-header">
          <div>
            <h2>2D Seat Map</h2>
            <p v-if="hasQueried">Available seats for this time slot: {{ availableSeats.length }}. Click any seat to view details, leave a comment, or reserve it.</p>
            <p v-else>Select a date to automatically load seat availability, then click any seat on the map to view details.</p>
          </div>
        </div>

        <div v-if="loadingSeats" class="feedback inline">Loading seat status...</div>
        <div v-else-if="hasQueried && !seatStatusList.length" class="feedback inline">No seat data is available for the selected time slot.</div>
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
            <h3>Seat Overview</h3>
            <div v-if="selectedSeat" class="selection-card">
              <p><strong>Seat:</strong> {{ selectedSeat.seatCode }}</p>
              <p><strong>Status:</strong> {{ seatStatusText(selectedSeat) }}</p>
              <p>
                <strong>Environment:</strong>
                {{ selectedSeatEnvironment.length ? selectedSeatEnvironment.join(', ') : 'Standard area' }}
              </p>
              <p>The detail panel is ready for comments and reservation actions.</p>
              <button class="primary-button full-width" @click="drawerVisible = true">Open Detail Panel</button>
            </div>
            <div v-else class="selection-empty">
              Click any seat on the map to view the seat code, status, coordinates, nearby environment, and comment board.
            </div>

            <div class="tips-card">
              <h4>Map Guide</h4>
              <ul>
                <li>Use doors, windows, and seat layout to understand the room zones.</li>
                <li>Blue means available, red means reserved, and gray means unavailable.</li>
                <li>Any seat can be opened in the detail panel for more information and comments.</li>
                <li>Reserved and unavailable seats can still be viewed and commented on.</li>
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
        :reserve-date="form.reserveDate"
        :start-time="form.startTime"
        :end-time="form.endTime"
        :swap-availability="swapAvailability"
        :checking-swap-availability="checkingSwapAvailability"
        :swap-submitting="swapSubmitting"
        @close="closeSeatDrawer"
        @confirm-reserve="reserveSeat"
        @request-swap="openSwapModal"
        @update-time-range="updateTimeRange"
        @timeline-hour-click="(hour) => handleTimelineHourClick(hour, 'seat')"
        @seat-timeline-loaded="handleSeatTimelineLoaded"
      />

      <SwapRequestMessageModal
        :visible="swapModalVisible"
        :seat-label="selectedSeatLabel"
        :schedule-text="selectedScheduleText"
        :submitting="swapSubmitting"
        @close="swapModalVisible = false"
        @confirm="submitSwapRequest"
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

.panel-heading p {
  margin: 8px 0 0;
  color: #64748b;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.compact-form-grid label {
  font-size: 14px;
  color: #64748b;
}

.compact-form-grid input,
.compact-form-grid select {
  background: #f8fafc;
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
