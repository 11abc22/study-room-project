<script setup>
function formatHour(hour) {
  return String(hour).padStart(2, '0')
}

defineProps({
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
  }
})

function getBlockClass(item) {
  if (!item) {
    return 'empty'
  }

  if (item.available === 0 || item.status === 'busy') {
    return 'full'
  }

  if (item.occupied > 0 || item.status === 'partial') {
    return 'partial'
  }

  return 'empty'
}
</script>

<template>
  <section class="timeline-card">
    <div v-if="loading" class="timeline-feedback">Loading room timeline...</div>
    <div v-else-if="error" class="timeline-feedback error">{{ error }}</div>
    <div v-else-if="!timeline.length" class="timeline-feedback">Select a date to view the room timeline.</div>
    <div v-else class="timeline-strip" aria-label="Room timeline">
      <div v-for="item in timeline" :key="item.hour" :class="['timeline-item', getBlockClass(item)]">
        <span class="timeline-hour">{{ formatHour(item.hour) }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.timeline-card {
  margin-top: 16px;
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
  min-height: 42px;
  border-right: 1px solid #d1d5db;
  background: #ffffff;
}

.timeline-item:last-child {
  border-right: none;
}

.timeline-item.partial {
  background: #fde68a;
}

.timeline-item.full {
  background: #ef4444;
}

.timeline-hour {
  position: absolute;
  left: 4px;
  bottom: 4px;
  font-size: 10px;
  line-height: 1;
  color: #475569;
}

.timeline-item.full .timeline-hour {
  color: #ffffff;
}

@media (max-width: 768px) {
  .timeline-item {
    min-height: 36px;
  }

  .timeline-hour {
    font-size: 9px;
    left: 3px;
    bottom: 3px;
  }
}
</style>
