<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  reservationTitle: {
    type: String,
    default: '--'
  },
  scheduleText: {
    type: String,
    default: '--'
  },
  requestData: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  acting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'approve', 'reject'])
</script>

<template>
  <transition name="modal-fade">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="modal-card">
        <header class="modal-header">
          <div>
            <p class="eyebrow">Seat Swap</p>
            <h3>Swap Request</h3>
          </div>
          <button class="icon-button" type="button" @click="emit('close')">×</button>
        </header>

        <div class="modal-body">
          <template v-if="loading">
            <div class="empty-state">Loading swap request...</div>
          </template>

          <template v-else-if="requestData">
            <div class="summary-card">
              <p><strong>Requester:</strong> {{ requestData.requesterName || `User ${requestData.requesterId}` }}</p>
              <p><strong>Target Seat:</strong> {{ reservationTitle }}</p>
              <p><strong>Schedule:</strong> {{ scheduleText }}</p>
            </div>

            <div class="message-card">
              <span>Message</span>
              <p>{{ requestData.message || 'No message was left.' }}</p>
            </div>
          </template>

          <template v-else>
            <div class="empty-state">No active swap request for this reservation.</div>
          </template>
        </div>

        <footer class="modal-actions">
          <button class="ghost-button" type="button" :disabled="acting" @click="emit('close')">Close</button>
          <template v-if="requestData">
            <button class="danger-button" type="button" :disabled="acting" @click="emit('reject')">
              {{ acting ? 'Processing...' : 'Reject' }}
            </button>
            <button class="primary-button" type="button" :disabled="acting" @click="emit('approve')">
              {{ acting ? 'Processing...' : 'Approve' }}
            </button>
          </template>
        </footer>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 1100;
}

.modal-card {
  width: min(560px, 100%);
  background: #fff;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.modal-header,
.modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
}

.modal-header {
  border-bottom: 1px solid #e5e7eb;
}

.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-weight: 600;
}

h3 {
  margin: 0;
}

.summary-card,
.message-card,
.empty-state {
  border-radius: 14px;
  padding: 16px;
}

.summary-card {
  border: 1px solid #dbeafe;
  background: #f8fbff;
}

.summary-card p,
.message-card p,
.empty-state {
  margin: 0;
  color: #475569;
}

.summary-card p + p {
  margin-top: 8px;
}

.message-card {
  border: 1px solid #e5e7eb;
  background: #fff;
}

.message-card span {
  display: block;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
}

.modal-actions {
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
}

.primary-button,
.ghost-button,
.danger-button,
.icon-button {
  border: none;
  border-radius: 10px;
  font: inherit;
  cursor: pointer;
}

.primary-button,
.ghost-button,
.danger-button {
  padding: 10px 16px;
  font-weight: 600;
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

.icon-button {
  width: 36px;
  height: 36px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 24px;
  line-height: 1;
}

.primary-button:disabled,
.ghost-button:disabled,
.danger-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.18s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
