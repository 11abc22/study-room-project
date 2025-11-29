<script setup>
import { computed } from 'vue'
import { createSeatMapLayout, getSeatEnvironmentTags } from '@/utils/seatMapLayout'

const props = defineProps({
  room: {
    type: Object,
    default: null
  },
  seatStatusList: {
    type: Array,
    default: () => []
  },
  mapConfig: {
    type: Object,
    required: true
  },
  selectedSeatId: {
    type: Number,
    default: null
  },
  reservingSeatId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['select-seat'])

const layout = computed(() => createSeatMapLayout(props.seatStatusList, props.mapConfig))
const columns = computed(() => layout.value.grid.columns)
const rows = computed(() => layout.value.grid.rows)
const cellSize = computed(() => layout.value.grid.cellSize)

const mapStyle = computed(() => ({
  '--map-columns': columns.value,
  '--map-rows': rows.value,
  '--cell-size': `${cellSize.value}px`
}))

const seatItems = computed(() =>
  layout.value.seats.map((seat) => ({
    ...seat,
    environmentTags: getSeatEnvironmentTags(seat, layout.value),
    isSelected: props.selectedSeatId === seat.seatId,
    isDisabled: seat.seatStatus !== 1,
    isReserved: seat.reserved,
    isClickable: true,
    canReserve: seat.seatStatus === 1 && !seat.reserved
  }))
)

const legendItems = computed(() => {
  const items = [
    { key: 'available', label: '空闲可预约' },
    { key: 'selected', label: '当前选中' },
    { key: 'reserved', label: '已预约' },
    { key: 'disabled', label: '不可用' },
    { key: 'door', label: '门口' },
    { key: 'window', label: '窗户' }
  ]

  if (layout.value.toilets?.length) {
    items.push({ key: 'toilet', label: '厕所' })
  }

  if (layout.value.aisles?.vertical?.length || layout.value.aisles?.horizontal?.length) {
    items.push({ key: 'aisle', label: '过道 / 分隔' })
  }

  if (layout.value.lightZones?.length) {
    items.push({ key: 'light', label: '采光提示区' })
  }

  return items
})

function emitSelectSeat(seat) {
  emit('select-seat', seat)
}

function featureStyle(feature) {
  if (feature.edge === 'top' || feature.edge === 'bottom') {
    return {
      gridColumn: `${feature.start} / span ${feature.span}`,
      gridRow: `${feature.line}`
    }
  }

  return {
    gridColumn: `${feature.line}`,
    gridRow: `${feature.start} / span ${feature.span}`
  }
}

function zoneStyle(zone) {
  return {
    gridColumn: `${zone.x} / span ${zone.width}`,
    gridRow: `${zone.y} / span ${zone.height}`
  }
}

function seatStyle(seat) {
  return {
    gridColumn: `${seat.displayX}`,
    gridRow: `${seat.displayY}`
  }
}
</script>

<template>
  <div class="seat-map-wrapper">
    <div class="legend">
      <span v-for="item in legendItems" :key="item.key" class="legend-item">
        <i :class="['legend-swatch', item.key]"></i>
        {{ item.label }}
      </span>
    </div>

    <div class="seat-map" :style="mapStyle">
      <div
        v-for="zone in layout.lightZones"
        :key="`light-${zone.label}-${zone.x}-${zone.y}`"
        class="map-zone light-zone"
        :style="zoneStyle(zone)"
      >
        {{ zone.label }}
      </div>

      <div
        v-for="label in layout.labels"
        :key="`label-${label.text}-${label.x}-${label.y}`"
        class="map-zone text-zone"
        :style="zoneStyle(label)"
      >
        {{ label.text }}
      </div>

      <div
        v-for="line in layout.aisles.vertical"
        :key="line.id"
        class="aisle vertical"
        :style="{ gridColumn: `${line.line} / span 1`, gridRow: `1 / span ${rows}` }"
      >
        过道
      </div>

      <div
        v-for="line in layout.aisles.horizontal"
        :key="line.id"
        class="aisle horizontal"
        :style="{ gridColumn: `1 / span ${columns}`, gridRow: `${line.line} / span 1` }"
      >
        过道
      </div>

      <div
        v-for="window in layout.windows"
        :key="`window-${window.label}-${window.edge}-${window.start}`"
        class="edge-feature window"
        :style="featureStyle(window)"
      >
        {{ window.label || '窗户' }}
      </div>

      <div
        v-for="door in layout.doors"
        :key="`door-${door.label}-${door.edge}-${door.start}`"
        class="edge-feature door"
        :style="featureStyle(door)"
      >
        {{ door.label || '门' }}
      </div>

      <div
        v-for="toilet in layout.toilets"
        :key="`toilet-${toilet.label}-${toilet.edge}-${toilet.start}`"
        class="edge-feature toilet"
        :style="featureStyle(toilet)"
      >
        {{ toilet.label || '厕所' }}
      </div>

      <button
        v-for="seat in seatItems"
        :key="seat.seatId"
        :class="[
          'seat-node',
          {
            available: seat.canReserve,
            reserved: seat.isReserved,
            disabled: seat.isDisabled,
            selected: seat.isSelected,
            viewable: !seat.canReserve,
            reserving: reservingSeatId === seat.seatId
          }
        ]"
        :style="seatStyle(seat)"
        :title="`${seat.seatCode} - ${seat.environmentTags.join(' / ') || '普通区域'}`"
        @click="emitSelectSeat(seat)"
      >
        <span class="seat-code">{{ seat.seatCode }}</span>
        <span class="seat-position">{{ seat.x }},{{ seat.y }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.seat-map-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #4b5563;
  font-size: 14px;
}

.legend-swatch {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  display: inline-block;
}

.legend-swatch.available {
  background: #dbeafe;
  border: 1px solid #60a5fa;
}

.legend-swatch.selected {
  background: #1d4ed8;
}

.legend-swatch.reserved {
  background: #fecaca;
}

.legend-swatch.disabled {
  background: #e5e7eb;
}

.legend-swatch.door {
  background: rgba(187, 247, 208, 0.5);
  border: 1px solid rgba(34, 197, 94, 0.25);
}

.legend-swatch.window {
  background: rgba(147, 197, 253, 0.28);
  border: 1px solid rgba(59, 130, 246, 0.25);
}

.legend-swatch.toilet {
  background: rgba(254, 215, 170, 0.52);
  border: 1px solid rgba(249, 115, 22, 0.24);
}

.legend-swatch.aisle {
  background: repeating-linear-gradient(135deg, #e5e7eb, #e5e7eb 6px, #f8fafc 6px, #f8fafc 12px);
}

.legend-swatch.light {
  background: rgba(250, 204, 21, 0.28);
}

.seat-map {
  position: relative;
  display: grid;
  grid-template-columns: repeat(var(--map-columns), minmax(0, var(--cell-size)));
  grid-template-rows: repeat(var(--map-rows), minmax(0, var(--cell-size)));
  gap: 10px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
  border: 1px solid #dbeafe;
  overflow: auto;
  justify-content: start;
  align-content: start;
}

.map-zone,
.aisle,
.edge-feature,
.seat-node {
  border-radius: 14px;
}

.map-zone {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 13px;
  line-height: 1.35;
  padding: 8px;
  z-index: 1;
  min-height: calc(var(--cell-size) * 0.8);
}

.light-zone {
  background: rgba(250, 204, 21, 0.24);
  color: #92400e;
  border: 1px dashed rgba(217, 119, 6, 0.35);
}

.text-zone {
  background: rgba(255, 255, 255, 0.9);
  color: #475569;
  border: 1px dashed #cbd5e1;
}

.aisle {
  display: flex;
  align-items: center;
  justify-content: center;
  background: repeating-linear-gradient(135deg, #e5e7eb, #e5e7eb 8px, #f8fafc 8px, #f8fafc 16px);
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  z-index: 2;
}

.aisle.vertical {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 2px;
}

.edge-feature {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  padding: 8px;
  z-index: 4;
  min-height: calc(var(--cell-size) * 0.7);
}

.edge-feature.window {
  background: rgba(147, 197, 253, 0.28);
  color: #1d4ed8;
  border: 1px solid rgba(59, 130, 246, 0.25);
}

.edge-feature.door {
  background: rgba(187, 247, 208, 0.5);
  color: #166534;
  border: 1px solid rgba(34, 197, 94, 0.25);
}

.edge-feature.toilet {
  background: rgba(254, 215, 170, 0.52);
  color: #9a3412;
  border: 1px solid rgba(249, 115, 22, 0.24);
}

.seat-node {
  z-index: 5;
  border: 1px solid transparent;
  padding: 8px 6px;
  min-width: calc(var(--cell-size) * 0.92);
  min-height: calc(var(--cell-size) * 0.92);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
  font: inherit;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}

.seat-node.available {
  background: #dbeafe;
  color: #1d4ed8;
  border-color: #60a5fa;
}

.seat-node.viewable:hover,
.seat-node.available:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.12);
}

.seat-node.selected {
  background: #1d4ed8;
  color: #fff;
  border-color: #1e3a8a;
  box-shadow: 0 12px 20px rgba(29, 78, 216, 0.24);
}

.seat-node.reserved {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fca5a5;
}

.seat-node.disabled {
  background: #e5e7eb;
  color: #6b7280;
  border-color: #d1d5db;
}

.seat-node.reserving {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.16);
}

.seat-code {
  font-weight: 700;
  font-size: 14px;
}

.seat-position {
  font-size: 11px;
  opacity: 0.85;
}

@media (max-width: 768px) {
  .seat-map {
    padding: 12px;
    gap: 6px;
  }

  .map-zone,
  .edge-feature {
    font-size: 12px;
  }
}
</style>
