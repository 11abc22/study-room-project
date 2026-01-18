export const RESERVATION_STATUS = Object.freeze({
  RESERVED: 'RESERVED',
  CANCELLED: 'CANCELLED',
  REQUESTING: 'REQUESTING',
  PENDING: 'PENDING'
})

const LEGACY_STATUS_MAP = Object.freeze({
  0: RESERVATION_STATUS.CANCELLED,
  1: RESERVATION_STATUS.RESERVED,
  2: RESERVATION_STATUS.CANCELLED,
  3: RESERVATION_STATUS.REQUESTING,
  4: RESERVATION_STATUS.PENDING
})

const STATUS_META = Object.freeze({
  [RESERVATION_STATUS.RESERVED]: {
    label: 'Reserved',
    className: 'reserved'
  },
  [RESERVATION_STATUS.CANCELLED]: {
    label: 'Cancelled',
    className: 'cancelled'
  },
  [RESERVATION_STATUS.REQUESTING]: {
    label: 'Requesting',
    className: 'requesting'
  },
  [RESERVATION_STATUS.PENDING]: {
    label: 'Pending',
    className: 'pending'
  }
})

export function normalizeReservationStatus(status) {
  if (typeof status === 'string') {
    const normalized = status.toUpperCase()

    if (STATUS_META[normalized]) {
      return normalized
    }

    if (normalized === 'CANCELLED_BY_ADMIN' || normalized === 'CANCELLED_BY_USER') {
      return RESERVATION_STATUS.CANCELLED
    }
  }

  if (typeof status === 'number' && LEGACY_STATUS_MAP[status]) {
    return LEGACY_STATUS_MAP[status]
  }

  return RESERVATION_STATUS.CANCELLED
}

export function getReservationStatusMeta(status) {
  const normalizedStatus = normalizeReservationStatus(status)
  return {
    value: normalizedStatus,
    ...STATUS_META[normalizedStatus]
  }
}

export function isReservedStatus(status) {
  return normalizeReservationStatus(status) === RESERVATION_STATUS.RESERVED
}

export function isRequestingStatus(status) {
  return normalizeReservationStatus(status) === RESERVATION_STATUS.REQUESTING
}

export function isPendingStatus(status) {
  return normalizeReservationStatus(status) === RESERVATION_STATUS.PENDING
}

export function isCancelledStatus(status) {
  return normalizeReservationStatus(status) === RESERVATION_STATUS.CANCELLED
}
