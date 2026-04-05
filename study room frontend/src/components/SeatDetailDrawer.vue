<script setup>
import { computed, ref, watch } from 'vue'
import { createSeatComment, getSeatComments } from '@/services/reservationApi'

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
  }
})

const emit = defineEmits(['close', 'confirm-reserve', 'comment-created'])

const comments = ref([])
const loadingComments = ref(false)
const commentsError = ref('')
const commentInput = ref('')
const commentSubmitting = ref(false)
const commentFeedback = ref('')
const commentFeedbackType = ref('success')

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

watch(
  () => [props.visible, props.seat?.seatId],
  async ([visible, seatId]) => {
    resetCommentFeedback()
    commentInput.value = ''
    comments.value = []
    commentsError.value = ''

    if (visible && seatId) {
      await loadComments()
    }
  },
  { immediate: false }
)

async function loadComments() {
  if (!props.seat?.seatId) {
    return
  }

  loadingComments.value = true
  commentsError.value = ''

  try {
    const { data } = await getSeatComments(props.seat.seatId)
    comments.value = data
  } catch (error) {
    comments.value = []
    commentsError.value = error.response?.data?.message || 'Failed to load comments. Please try again later.'
  } finally {
    loadingComments.value = false
  }
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
            <p v-if="reserveDisabledReason" class="hint warning">{{ reserveDisabledReason }}</p>
            <button
              class="primary-button full-width"
              :disabled="!canReserve || reservingSeatId === seat.seatId"
              @click="handleReserve"
            >
              {{ reservingSeatId === seat.seatId ? 'Reserving...' : 'Confirm Reservation' }}
            </button>
          </section>

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

textarea {
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px;
  font: inherit;
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

  .comment-header,
  .comment-form-footer,
  .comment-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
