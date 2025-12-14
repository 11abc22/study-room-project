const DEFAULT_MAP_CONFIG = {
  layoutMode: 'normalized',
  grid: {
    columns: 14,
    rows: 9,
    cellSize: 64
  },
  aisles: {
    vertical: [],
    horizontal: []
  },
  windows: [
    { edge: 'top', start: 2, span: 6, label: 'Window' }
  ],
  doors: [
    { edge: 'bottom', start: 6, span: 3, label: 'Door' }
  ],
  toilets: [],
  lightZones: [],
  labels: []
}

const ROOM_MAP_CONFIG = {
  1: {
    layoutMode: 'manual-grid',
    grid: {
      columns: 10,
      rows: 6,
      cellSize: 72
    },
    manualSeatGrid: {
      columns: [2, 4, 6, 8],
      rows: [2, 3],
      overflowStartRow: 4
    },
    aisles: {
      vertical: [],
      horizontal: []
    },
    doors: [
      { edge: 'top', start: 2, span: 2, label: 'Door' }
    ],
    windows: [
      { edge: 'bottom', start: 2, span: 6, label: 'Window' }
    ],
    toilets: [],
    lightZones: [],
    labels: []
  },
  2: {
    layoutMode: 'normalized',
    grid: {
      columns: 16,
      rows: 11,
      cellSize: 62
    },
    aisles: {
      vertical: [5, 11],
      horizontal: []
    },
    windows: [
      { edge: 'top', start: 60, span: 220, label: 'Window' }
    ],
    doors: [
      { edge: 'bottom', start: 140, span: 90, label: 'Door' }
    ],
    toilets: [
      { edge: 'right', start: 1, span: 110, label: 'Toilet' }
    ],
    lightZones: [],
    labels: []
  }
}

function cloneConfig(config) {
  return JSON.parse(JSON.stringify(config))
}

export function getRoomMapConfig(roomId) {
  const defaultConfig = cloneConfig(DEFAULT_MAP_CONFIG)
  const roomConfig = cloneConfig(ROOM_MAP_CONFIG[roomId] || {})

  return {
    ...defaultConfig,
    ...roomConfig,
    grid: {
      ...defaultConfig.grid,
      ...(roomConfig.grid || {})
    },
    manualSeatGrid: roomConfig.manualSeatGrid || defaultConfig.manualSeatGrid,
    aisles: {
      ...(defaultConfig.aisles || {}),
      ...(roomConfig.aisles || {})
    },
    windows: roomConfig.windows ?? defaultConfig.windows,
    doors: roomConfig.doors ?? defaultConfig.doors,
    toilets: roomConfig.toilets ?? defaultConfig.toilets,
    lightZones: roomConfig.lightZones ?? defaultConfig.lightZones,
    labels: roomConfig.labels ?? defaultConfig.labels
  }
}
