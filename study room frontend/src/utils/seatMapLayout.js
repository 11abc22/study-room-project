function getMedian(values) {
  const sorted = [...values].sort((a, b) => a - b)
  const middle = Math.floor(sorted.length / 2)

  if (sorted.length % 2 === 0) {
    return (sorted[middle - 1] + sorted[middle]) / 2
  }

  return sorted[middle]
}

function naturalSeatSort(a, b) {
  const codeCompare = String(a.seatCode || '').localeCompare(String(b.seatCode || ''), 'zh-Hans-CN', {
    numeric: true,
    sensitivity: 'base'
  })

  if (codeCompare !== 0) {
    return codeCompare
  }

  return Number(a.seatId || 0) - Number(b.seatId || 0)
}

function normalizeAxis(values, options = {}) {
  const {
    thresholdMultiplier = 1.8,
    maxBridgeSlots = 1,
    baseStep = 2,
    minGapValue = 10
  } = options

  const sortedValues = [...new Set(values)]
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b)

  if (!sortedValues.length) {
    return {
      positions: new Map(),
      order: [],
      max: 0,
      gaps: []
    }
  }

  const diffs = []
  for (let index = 1; index < sortedValues.length; index += 1) {
    diffs.push(sortedValues[index] - sortedValues[index - 1])
  }

  const positiveDiffs = diffs.filter((diff) => diff > 0)
  const medianDiff = positiveDiffs.length ? getMedian(positiveDiffs) : 1
  const threshold = Math.max(medianDiff * thresholdMultiplier, minGapValue)

  const positions = new Map()
  const gaps = []
  let cursor = 2
  positions.set(sortedValues[0], cursor)

  for (let index = 1; index < sortedValues.length; index += 1) {
    const previous = sortedValues[index - 1]
    const current = sortedValues[index]
    const rawGap = current - previous
    const extraSlots = rawGap > threshold
      ? Math.min(maxBridgeSlots, 1)
      : 0

    cursor += baseStep + extraSlots
    positions.set(current, cursor)

    if (extraSlots > 0) {
      gaps.push({
        line: cursor - 1,
        size: extraSlots
      })
    }
  }

  return {
    positions,
    order: sortedValues,
    max: cursor + 1,
    gaps
  }
}

function findClosest(values, target) {
  return values.reduce((closest, current) => (
    Math.abs(current - target) < Math.abs(closest - target) ? current : closest
  ), values[0])
}

function resolveSpan(feature, axis, fallback = 2) {
  const axisKeys = axis.order
  if (!axisKeys.length) {
    return fallback
  }

  const startRaw = feature.start
  const rawEnd = startRaw + Math.max(feature.span || 1, 1) - 1
  const covered = axisKeys.filter((value) => value >= startRaw && value <= rawEnd)
  const firstCovered = covered[0] ?? findClosest(axisKeys, startRaw)
  const lastCovered = covered[covered.length - 1] ?? firstCovered
  const displayStart = axis.positions.get(firstCovered)
  const displayEnd = axis.positions.get(lastCovered)

  return {
    start: displayStart,
    span: Math.max(2, displayEnd - displayStart + 2)
  }
}

function mapEdgeFeature(feature, axis, maxPosition) {
  const resolved = resolveSpan(feature, axis, 2)

  if (!resolved.start) {
    return null
  }

  return {
    ...feature,
    start: resolved.start,
    span: resolved.span,
    line: feature.edge === 'top' || feature.edge === 'left' ? 1 : maxPosition
  }
}

function mapRect(rect, xAxis, yAxis) {
  const x = xAxis.positions.get(rect.x)
  const y = yAxis.positions.get(rect.y)

  if (!x || !y) {
    return null
  }

  return {
    ...rect,
    x,
    y,
    width: Math.max(2, Math.round(rect.width || 2)),
    height: Math.max(1, Math.round(rect.height || 1))
  }
}

function mapDisplayEdgeFeature(feature, columns, rows) {
  return {
    ...feature,
    line: feature.edge === 'top' || feature.edge === 'left' ? 1 : feature.edge === 'bottom' ? rows : columns
  }
}

function createManualGridLayout(seatStatusList = [], baseConfig = {}) {
  const manualSeatGrid = baseConfig.manualSeatGrid || {}
  const columns = manualSeatGrid.columns?.length ? manualSeatGrid.columns : [2, 4, 6, 8]
  const preferredRows = manualSeatGrid.rows?.length ? manualSeatGrid.rows : [2, 3]
  const overflowStartRow = manualSeatGrid.overflowStartRow || Math.max(...preferredRows, 3) + 1
  const orderedSeats = [...seatStatusList].sort(naturalSeatSort)

  const seats = orderedSeats.map((seat, index) => {
    const columnIndex = index % columns.length
    const rowIndex = Math.floor(index / columns.length)
    const displayRow = rowIndex < preferredRows.length
      ? preferredRows[rowIndex]
      : overflowStartRow + (rowIndex - preferredRows.length)

    return {
      ...seat,
      displayX: columns[columnIndex],
      displayY: displayRow
    }
  })

  const maxSeatRow = seats.length ? Math.max(...seats.map((seat) => seat.displayY)) : 3
  const configuredRows = baseConfig.grid?.rows || 6
  const rows = Math.max(configuredRows, maxSeatRow + 2)
  const layout = {
    grid: {
      columns: baseConfig.grid?.columns || 10,
      rows,
      cellSize: baseConfig.grid?.cellSize || 72
    },
    seats,
    aisles: {
      vertical: [],
      horizontal: []
    },
    windows: (baseConfig.windows || []).map((feature) => mapDisplayEdgeFeature(feature, baseConfig.grid?.columns || 10, rows)),
    doors: (baseConfig.doors || []).map((feature) => mapDisplayEdgeFeature(feature, baseConfig.grid?.columns || 10, rows)),
    toilets: (baseConfig.toilets || []).map((feature) => mapDisplayEdgeFeature(feature, baseConfig.grid?.columns || 10, rows)),
    lightZones: (baseConfig.lightZones || []).map((zone) => ({ ...zone })),
    labels: (baseConfig.labels || []).map((label) => ({ ...label }))
  }

  return layout
}

function createNormalizedLayout(seatStatusList = [], baseConfig = {}) {
  const xAxis = normalizeAxis(seatStatusList.map((seat) => seat.x), { minGapValue: 12 })
  const yAxis = normalizeAxis(seatStatusList.map((seat) => seat.y), { minGapValue: 8 })

  const seats = seatStatusList.map((seat) => ({
    ...seat,
    displayX: xAxis.positions.get(seat.x) || 2,
    displayY: yAxis.positions.get(seat.y) || 2
  }))

  const columns = Math.max(baseConfig.grid?.columns || 0, xAxis.max + 1, 10)
  const rows = Math.max(baseConfig.grid?.rows || 0, yAxis.max + 1, 8)

  return {
    grid: {
      columns,
      rows,
      cellSize: baseConfig.grid?.cellSize || 62
    },
    seats,
    aisles: {
      vertical: xAxis.gaps,
      horizontal: yAxis.gaps
    },
    windows: (baseConfig.windows || [])
      .map((feature) => mapEdgeFeature(feature, feature.edge === 'top' || feature.edge === 'bottom' ? xAxis : yAxis, feature.edge === 'top' || feature.edge === 'left' ? 1 : feature.edge === 'bottom' ? rows : columns))
      .filter(Boolean),
    doors: (baseConfig.doors || [])
      .map((feature) => mapEdgeFeature(feature, feature.edge === 'top' || feature.edge === 'bottom' ? xAxis : yAxis, feature.edge === 'top' || feature.edge === 'left' ? 1 : feature.edge === 'bottom' ? rows : columns))
      .filter(Boolean),
    toilets: (baseConfig.toilets || [])
      .map((feature) => mapEdgeFeature(feature, feature.edge === 'top' || feature.edge === 'bottom' ? xAxis : yAxis, feature.edge === 'top' || feature.edge === 'left' ? 1 : feature.edge === 'bottom' ? rows : columns))
      .filter(Boolean),
    lightZones: (baseConfig.lightZones || []).map((zone) => mapRect(zone, xAxis, yAxis)).filter(Boolean),
    labels: (baseConfig.labels || []).map((label) => mapRect(label, xAxis, yAxis)).filter(Boolean)
  }
}

export function createSeatMapLayout(seatStatusList = [], baseConfig = {}) {
  if (baseConfig.layoutMode === 'manual-grid') {
    return createManualGridLayout(seatStatusList, baseConfig)
  }

  return createNormalizedLayout(seatStatusList, baseConfig)
}

function hasAisleBetween(aisles, value1, value2) {
  const minValue = Math.min(value1, value2)
  const maxValue = Math.max(value1, value2)
  return aisles.some((aisle) => aisle.line > minValue && aisle.line < maxValue)
}

function isInsideRect(x, y, rect) {
  return x >= rect.x && x < rect.x + rect.width && y >= rect.y && y < rect.y + rect.height
}

function isNearEdgeFeature(x, y, feature, distance = 1) {
  if (feature.edge === 'top') {
    return y <= distance + 1 && x >= feature.start && x < feature.start + feature.span
  }

  if (feature.edge === 'bottom') {
    return y >= feature.line - distance && x >= feature.start && x < feature.start + feature.span
  }

  if (feature.edge === 'left') {
    return x <= distance + 1 && y >= feature.start && y < feature.start + feature.span
  }

  if (feature.edge === 'right') {
    return x >= feature.line - distance && y >= feature.start && y < feature.start + feature.span
  }

  return false
}

export function getSeatEnvironmentTags(seat, layout) {
  const tags = []
  const { displayX, displayY, seatId } = seat

  if ((layout.windows || []).some((window) => isNearEdgeFeature(displayX, displayY, window))) {
    tags.push('靠窗')
  }

  if ((layout.toilets || []).some((toilet) => isNearEdgeFeature(displayX, displayY, toilet, 2))) {
    tags.push('靠近厕所')
  }

  const lightZone = (layout.lightZones || []).find((zone) => isInsideRect(displayX, displayY, zone))
  if (lightZone) {
    tags.push(lightZone.label)
  }

  const horizontalNeighbor = (layout.seats || []).some(
    (item) => item.seatId !== seatId && item.displayY === displayY && Math.abs(item.displayX - displayX) === 2 && !hasAisleBetween(layout.aisles?.vertical || [], item.displayX, displayX)
  )

  if (horizontalNeighbor) {
    tags.push('与相邻座位同排')
  }

  const verticalNeighbor = (layout.seats || []).some(
    (item) => item.seatId !== seatId && item.displayX === displayX && Math.abs(item.displayY - displayY) === 1 && !hasAisleBetween(layout.aisles?.horizontal || [], item.displayY, displayY)
  )

  if (verticalNeighbor) {
    tags.push('与上下排座位对齐')
  }

  return [...new Set(tags)]
}
