<script setup>
import { computed, ref, watch } from 'vue'
import SeatTimeline from '@/components/SeatTimeline.vue'
import { createSeatComment, getSeatComments, getSeatTimeline } from '@/services/reservationApi'

const timeOptions = Array.from({ length: 16 }, (_, index) => {
  const hour = index + 8
  return {
    label: `${String(hour).padStart(2, '0')}:00`,
    value: `${String(hour).padStart(2, '0')}:00`
  }
})

function toHour(value) {
  if (!value) {
    return null
  }

  const [hour] = value.split(':')
  return Number(hour)
}

function toTimeString(hour) {
  return `${String(hour).padStart(2, '0')}:00`
}

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  seat: {
    type: Object,
    default: null
  },
  environmentTags: {
    type: Array,
    default: () => []
  },
  loadingSeatStatus: {
    type: Boolean,
    default: false
  },
  reservingSeatId: {
    type: Number,
    default: null
  },
  reserveDate: {
    type: String,
    default: ''
  },
  startTime: {
    type: String,
    default: ''
  },
  endTime: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'confirm-reserve', 'comment-created', 'update-time-range', 'timeline-hour-click', 'seat-timeline-loaded'])

const comments = ref([])
const loadingComments = ref(false)
const commentsError = ref('')
const commentInput = ref('')
const commentSubmitting = ref(false)
const commentFeedback = ref('')
const commentFeedbackType = ref('success')
const seatTimeline = ref([])
const loadingSeatTimeline = ref(false)
const seatTimelineError = ref('')
const lastCommentsSeatId = ref(null)
const lastTimelineRequestKey = ref('')
let commentsRequestToken = 0
let timelineRequestToken = 0

const maxCommentLength = 50
const maxCommentCount = 10

const seatStatusText = computed(() => {
  if (!props.seat) {
    return 'No seat selected'
  }

  if (props.seat.seatStatus !== 1) {
    return 'Unavailable'
  }

  return props.seat.reserved ? 'Reserved' : 'Available'
})

const reserveDisabledReason = computed(() => {
  if (!props.seat) {
    return 'Please select a seat first.'
  }

  if (props.seat.seatStatus !== 1) {
    return 'This seat is not reservable right now, but you can still view details and leave a comment.'
  }

  if (props.seat.reserved) {
    return 'This seat is already reserved for the selected time slot. You can still leave a comment.'
  }

  return ''
})

const canReserve = computed(() => Boolean(props.seat) && !reserveDisabledReason.value)
const remainingCharacters = computed(() => maxCommentLength - commentInput.value.length)
const selectedStartHour = computed(() => toHour(props.startTime))
const selectedEndHour = computed(() => toHour(props.endTime))
const selectedTimeRangeText = computed(() => {
  if (!props.reserveDate) {
    return 'Select a date first'
  }

  if (!props.startTime || !props.endTime) {
    return 'Choose a start time and an end time'
  }

  return `${props.reserveDate} ${props.startTime} - ${props.endTime}`
})

watch(
  () => [props.visible, props.seat?.seatId, props.reserveDate],
  async ([visible, seatId, reserveDate], [previousVisible, previousSeatId, previousReserveDate]) => {
    const opened = visible && !previousVisible
    const seatChanged = seatId !== previousSeatId
    const dateChanged = reserveDate !== previousReserveDate

    if (!visible) {
      return
    }

    if (opened || seatChanged) {
      resetCommentFeedback()
      commentInput.value = ''
      comments.value = []
      commentsError.value = ''
      lastCommentsSeatId.value = null
      await loadComments({ force: true })
    }

    if (!reserveDate) {
      seatTimeline.value = []
      seatTimelineError.value = ''
      lastTimelineRequestKey.value = ''
      return
    }

    if (opened || seatChanged || dateChanged) {
      await loadSeatTimeline({ force: true })
    }
  },
  { immediate: false }
)

async function loadComments(options = {}) {
  const seatId = props.seat?.seatId

  if (!seatId) {
    return
  }

  if (!options.force && lastCommentsSeatId.value === seatId) {
    return
  }

  const requestToken = ++commentsRequestToken
  loadingComments.value = true
  commentsError.value = ''

  try {
    const { data } = await getSeatComments(seatId)

    if (requestToken !== commentsRequestToken) {
      return
    }

    comments.value = data
    lastCommentsSeatId.value = seatId
  } catch (error) {
    if (requestToken !== commentsRequestToken) {
      return
    }

    comments.value = []
    commentsError.value = error.response?.data?.message || 'Failed to load comments. Please try again later.'
  } finally {
    if (requestToken === commentsRequestToken) {
      loadingComments.value = false
    }
  }
}

async function loadSeatTimeline(options = {}) {
  const seatId = props.seat?.seatId
  const reserveDate = props.reserveDate

  if (!seatId || !reserveDate) {
    seatTimeline.value = []
    seatTimelineError.value = ''
    lastTimelineRequestKey.value = ''
    return
  }

  const requestKey = `${seatId}-${reserveDate}`
  if (!options.force && lastTimelineRequestKey.value === requestKey) {
    return
  }

  const requestToken = ++timelineRequestToken
  seatTimeline.value = []
  seatTimelineError.value = ''
  loadingSeatTimeline.value = true

  try {
    const { data } = await getSeatTimeline(seatId, reserveDate)

    if (requestToken !== timelineRequestToken) {
      return
    }

    seatTimeline.value = data
    lastTimelineRequestKey.value = requestKey
    emit('seat-timeline-loaded', data)
  } catch (error) {
    if (requestToken !== timelineRequestToken) {
      return
    }

    seatTimelineError.value = error.response?.data?.message || 'Failed to load seat timeline. Please try again later.'
  } finally {
    if (requestToken === timelineRequestToken) {
      loadingSeatTimeline.value = false
    }
  }
}

function emitTimeRange(startTime, endTime) {
  emit('update-time-range', {
    startTime,
    endTime
  })
}

function handleStartTimeChange(event) {
  const nextStartTime = event.target.value
  const nextStartHour = toHour(nextStartTime)
  const currentEndHour = selectedEndHour.value

  if (currentEndHour === null || nextStartHour >= currentEndHour) {
    emitTimeRange(nextStartTime, toTimeString(Math.min(nextStartHour + 1, 23)))
    return
  }

  emitTimeRange(nextStartTime, props.endTime)
}

function handleEndTimeChange(event) {
  const nextEndTime = event.target.value
  const nextEndHour = toHour(nextEndTime)
  const currentStartHour = selectedStartHour.value

  if (currentStartHour === null || nextEndHour <= currentStartHour) {
    emitTimeRange(toTimeString(Math.max(nextEndHour - 1, 8)), nextEndTime)
    return
  }

  emitTimeRange(props.startTime, nextEndTime)
}

function handleTimelineHourClick(hour) {
  emit('timeline-hour-click', hour)
}

async function submitComment() {
  if (!props.seat?.seatId || commentSubmitting.value) {
    return
  }

  resetCommentFeedback()
  const content = commentInput.value.trim()

  if (!content) {
    setCommentFeedback('Comment content cannot be empty.', 'error')
    return
  }

  if (content.length > maxCommentLength) {
    setCommentFeedback('Each comment must be 50 characters or fewer.', 'error')
    return
  }

  commentSubmitting.value = true

  try {
    await createSeatComment(props.seat.seatId, { content })
    commentInput.value = ''
    setCommentFeedback('Comment posted successfully.', 'success')
    await loadComments()
    emit('comment-created')
  } catch (error) {
    setCommentFeedback(error.response?.data?.message || 'Failed to post the comment. Please try again later.', 'error')
  } finally {
    commentSubmitting.value = false
  }
}

function handleReserve() {
  if (!canReserve.value || !props.seat) {
    return
  }

  emit('confirm-reserve', props.seat)
}

function setCommentFeedback(message, type) {
  commentFeedback.value = message
  commentFeedbackType.value = type
}

function resetCommentFeedback() {
  commentFeedback.value = ''
  commentFeedbackType.value = 'success'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }

  return new Date(value).toLocaleString('en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <transition name="drawer-fade">
    <div v-if="visible" class="drawer-overlay" @click.self="emit('close')">
      <aside class="drawer-panel">
        <header class="drawer-header">
          <div>
            <p class="drawer-eyebrow">Seat Details</p>
            <h3>{{ seat?.seatCode || '--' }}</h3>
          </div>
          <button class="icon-button" type="button" @click="emit('close')">×</button>
        </header>

        <div v-if="seat" class="drawer-body">
          <section class="info-card">
            <p><strong>Seat:</strong> {{ seat.seatCode }}</p>
            <p><strong>Status:</strong> {{ loadingSeatStatus ? 'Refreshing...' : seatStatusText }}</p>
            <p><strong>Environment:</strong> {{ environmentTags.length ? environmentTags.join(', ') : 'Standard area' }}</p>
            <div class="selected-time-card">
              <span class="selected-time-label">Current Selection</span>
              <strong>{{ selectedTimeRangeText }}</strong>
              <p>Click the timeline to set the start and end hours. The selectors below are kept as auxiliary adjustments.</p>
            </div>
            <div class="time-select-grid">
              <label>
                <span>Start Time</span>
                <select :value="startTime" @change="handleStartTimeChange">
                  <option v-for="option in timeOptions.slice(0, -1)" :key="`start-${option.value}`" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>End Time</span>
                <select :value="endTime" @change="handleEndTimeChange">
                  <option v-for="option in timeOptions.slice(1)" :key="`end-${option.value}`" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </div>
            <p v-if="reserveDisabledReason" class="hint warning">{{ reserveDisabledReason }}</p>
            <button
              class="primary-button full-width"
              :disabled="!canReserve || reservingSeatId === seat.seatId"
              @click="handleReserve"
            >
              {{ reservingSeatId === seat.seatId ? 'Reserving...' : 'Confirm Reservation' }}
            </button>
          </section>

          <SeatTimeline
            :timeline="seatTimeline"
            :loading="loadingSeatTimeline"
            :error="seatTimelineError"
            :selected-start-hour="selectedStartHour"
            :selected-end-hour="selectedEndHour"
            @hour-click="handleTimelineHourClick"
          />

          <section class="info-card comment-card">
            <div class="comment-header">
              <div>
                <h4>Comment Board</h4>
                <p>Up to {{ maxCommentCount }} recent comments are kept, with a {{ maxCommentLength }}-character limit each.</p>
              </div>
              <button class="text-button" type="button" :disabled="loadingComments" @click="loadComments">
                {{ loadingComments ? 'Loading...' : 'Refresh' }}
              </button>
            </div>

            <div class="comment-form">
              <textarea
                v-model="commentInput"
                :maxlength="maxCommentLength"
                rows="3"
                placeholder="Leave a comment for this seat"
              ></textarea>
              <div class="comment-form-footer">
                <span class="char-count" :class="{ danger: remainingCharacters < 10 }">
                  {{ remainingCharacters }} characters left
                </span>
                <button class="primary-button" type="button" :disabled="commentSubmitting" @click="submitComment">
                  {{ commentSubmitting ? 'Posting...' : 'Post Comment' }}
                </button>
              </div>
            </div>

            <div v-if="commentFeedback" :class="['feedback', commentFeedbackType === 'error' ? 'error' : 'success']">
              {{ commentFeedback }}
            </div>
            <div v-if="commentsError" class="feedback error">{{ commentsError }}</div>
            <div v-else-if="loadingComments" class="feedback inline">Loading comments...</div>
            <div v-else-if="!comments.length" class="empty-state">No comments yet. Be the first to leave one.</div>
            <ul v-else class="comment-list">
              <li v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="comment-meta">
                  <strong>{{ comment.username || `User ${comment.userId}` }}</strong>
                  <span>{{ formatDateTime(comment.createdAt) }}</span>
                </div>
                <p>{{ comment.content }}</p>
              </li>
            </ul>
          </section>
        </div>
      </aside>
    </div>
  </transition>
</template>

<style scoped>
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.32);
  display: flex;
  justify-content: flex-end;
  z-index: 1000;
}

.drawer-panel {
  width: min(480px, 100%);
  height: 100%;
  background: #fff;
  box-shadow: -12px 0 30px rgba(15, 23, 42, 0.16);
  display: flex;
  flex-direction: column;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 24px 24px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.drawer-eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-weight: 600;
}

.drawer-header h3,
.comment-header h4 {
  margin: 0;
}

.drawer-body {
  padding: 20px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.info-card {
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 14px;
  padding: 18px;
}

.info-card p {
  color: #475569;
}

.selected-time-card {
  margin: 14px 0;
  padding: 12px 14px;
  border-radius: 12px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.selected-time-card strong {
  display: block;
  margin-top: 4px;
  color: #1e3a8a;
}

.selected-time-label {
  font-size: 12px;
  color: #2563eb;
  font-weight: 600;
}

.selected-time-card p {
  margin: 8px 0 0;
  font-size: 13px;
}

.time-select-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.time-select-grid label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #334155;
  font-weight: 600;
}

.time-select-grid span {
  font-size: 13px;
}

.comment-header,
.comment-form-footer,
.comment-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.comment-header p,
.comment-meta span,
.hint,
.char-count {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.comment-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

select,
textarea {
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px;
  font: inherit;
  background: #fff;
}

textarea {
  resize: vertical;
}

.primary-button,
.text-button,
.icon-button {
  border: none;
  border-radius: 10px;
  font: inherit;
  cursor: pointer;
}

.primary-button {
  padding: 10px 16px;
  background: #2563eb;
  color: #fff;
  font-weight: 600;
}

.primary-button:disabled,
.text-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.text-button {
  background: transparent;
  color: #2563eb;
  padding: 0;
}

.icon-button {
  width: 36px;
  height: 36px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 24px;
  line-height: 1;
}

.full-width {
  width: 100%;
}

.feedback {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px 14px;
}

.feedback.inline,
.empty-state {
  color: #475569;
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

.hint.warning,
.char-count.danger {
  color: #b45309;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  background: #fff;
}

.comment-item p {
  margin: 10px 0 0;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.18s ease;
}

.drawer-fade-enter-active .drawer-panel,
.drawer-fade-leave-active .drawer-panel {
  transition: transform 0.18s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-fade-enter-from .drawer-panel,
.drawer-fade-leave-to .drawer-panel {
  transform: translateX(24px);
}

@media (max-width: 768px) {
  .drawer-panel {
    width: 100%;
  }

  .time-select-grid,
  .comment-header,
  .comment-form-footer,
  .comment-meta {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
