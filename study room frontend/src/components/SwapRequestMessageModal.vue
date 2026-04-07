<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  seatLabel: {
    type: String,
    default: '--'
  },
  scheduleText: {
    type: String,
    default: '--'
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'confirm'])

const message = ref('')
const maxLength = 50
const charCount = computed(() => message.value.length)

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      message.value = ''
    }
  }
)

function handleConfirm() {
  emit('confirm', message.value.trim())
}
</script>

<template>
  <transition name="modal-fade">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="modal-card">
        <header class="modal-header">
          <div>
            <p class="eyebrow">Seat Swap</p>
            <h3>Request a Seat Swap</h3>
          </div>
          <button class="icon-button" type="button" @click="emit('close')">×</button>
        </header>

        <div class="modal-body">
          <div class="summary-card">
            <p><strong>Target Seat:</strong> {{ seatLabel }}</p>
            <p><strong>Schedule:</strong> {{ scheduleText }}</p>
          </div>

          <label class="message-field">
            <span>Message (optional)</span>
            <textarea
              v-model="message"
              :maxlength="maxLength"
              rows="4"
              placeholder="Leave a short message to the seat holder"
            ></textarea>
            <small>{{ charCount }}/{{ maxLength }}</small>
          </label>
        </div>

        <footer class="modal-actions">
          <button class="ghost-button" type="button" :disabled="submitting" @click="emit('close')">Cancel</button>
          <button class="primary-button" type="button" :disabled="submitting" @click="handleConfirm">
            {{ submitting ? 'Sending...' : 'Send Request' }}
          </button>
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
  width: min(520px, 100%);
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

.summary-card {
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 14px;
  padding: 16px;
}

.summary-card p,
.message-field small {
  margin: 0;
  color: #475569;
}

.summary-card p + p {
  margin-top: 8px;
}

.message-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #334155;
  font-weight: 600;
}

textarea {
  resize: vertical;
  min-height: 110px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px;
  font: inherit;
}

.modal-actions {
  border-top: 1px solid #e5e7eb;
  justify-content: flex-end;
}

.primary-button,
.ghost-button,
.icon-button {
  border: none;
  border-radius: 10px;
  font: inherit;
  cursor: pointer;
}

.primary-button,
.ghost-button {
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

.icon-button {
  width: 36px;
  height: 36px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 24px;
  line-height: 1;
}

.primary-button:disabled,
.ghost-button:disabled {
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
