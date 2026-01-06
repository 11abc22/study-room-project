<script setup>
const props = defineProps({
  timeline: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  selectedStartHour: {
    type: Number,
    default: null
  },
  selectedEndHour: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['hour-click'])

function formatHour(hour) {
  return String(hour).padStart(2, '0')
}

function isReserved(item) {
  return item?.available === false
}

function isPending(item) {
  if (isReserved(item)) {
    return false
  }

  return props.selectedStartHour !== null
    && props.selectedEndHour !== null
    && item.hour >= props.selectedStartHour
    && item.hour < props.selectedEndHour
}

function handleHourClick(item) {
  emit('hour-click', item.hour)
}
</script>

<template>
  <section class="timeline-card">
    <div v-if="loading" class="timeline-feedback">Loading seat timeline...</div>
    <div v-else-if="error" class="timeline-feedback error">{{ error }}</div>
    <div v-else-if="!timeline.length" class="timeline-feedback">Select a date to view this seat timeline.</div>
    <div v-else class="timeline-strip" aria-label="Seat timeline">
      <button
        v-for="item in timeline"
        :key="item.hour"
        type="button"
        :class="[
          'timeline-item',
          {
            reserved: isReserved(item),
            pending: isPending(item),
            clickable: !loading
          }
        ]"
        @click="handleHourClick(item)"
      >
        <span class="timeline-hour">{{ formatHour(item.hour) }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.timeline-card {
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 14px;
  padding: 16px;
}

.timeline-feedback {
  color: #475569;
  font-size: 14px;
}

.timeline-feedback.error {
  color: #b91c1c;
}

.timeline-strip {
  display: grid;
  grid-template-columns: repeat(15, minmax(0, 1fr));
  gap: 0;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.timeline-item {
  position: relative;
  min-height: 40px;
  border: none;
  border-right: 1px solid #d1d5db;
  background: #ffffff;
  padding: 0;
}

.timeline-item:last-child {
  border-right: none;
}

.timeline-item.clickable {
  cursor: pointer;
}

.timeline-item.reserved {
  background: #ef4444;
}

.timeline-item.pending {
  background: #3b82f6;
}

.timeline-hour {
  position: absolute;
  left: 4px;
  bottom: 4px;
  font-size: 10px;
  line-height: 1;
  color: #475569;
}

.timeline-item.reserved .timeline-hour,
.timeline-item.pending .timeline-hour {
  color: #ffffff;
}

@media (max-width: 768px) {
  .timeline-item {
    min-height: 34px;
  }

  .timeline-hour {
    font-size: 9px;
    left: 3px;
    bottom: 3px;
  }
}
</style>
