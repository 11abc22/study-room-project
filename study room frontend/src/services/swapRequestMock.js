import apiClient from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { RESERVATION_STATUS, isReservedStatus, normalizeReservationStatus } from '@/constants/reservationStatus'

const STORAGE_KEY = 'study-room.swap-requests'

const SWAP_REQUEST_STATE = Object.freeze({
  REQUESTING: 'REQUESTING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  WITHDRAWN: 'WITHDRAWN',
  EXPIRED: 'EXPIRED'
})

function nowIso() {
  return new Date().toISOString()
}

function toNumber(value) {
  const result = Number(value)
  return Number.isFinite(result) ? result : null
}

function formatTime(value) {
  if (!value) {
    return ''
  }

  return String(value).slice(0, 5)
}

function buildReservationKey(reservationLike) {
  return [
    reservationLike.roomId ?? '',
    reservationLike.seatId ?? '',
    reservationLike.reserveDate ?? '',
    formatTime(reservationLike.startTime),
    formatTime(reservationLike.endTime)
  ].join('|')
}

function getStoredSwapRequests() {
  const raw = localStorage.getItem(STORAGE_KEY)

  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    console.warn('Failed to parse swap request mock data:', error)
    localStorage.removeItem(STORAGE_KEY)
    return []
  }
}

function saveStoredSwapRequests(requests) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(requests))
}

function getCurrentUser() {
  const authStore = useAuthStore()
  return authStore.user || null
}

function getCurrentUserId() {
  return toNumber(getCurrentUser()?.id)
}

function getCurrentUserName() {
  const user = getCurrentUser()
  return user?.username || user?.name || user?.email || (user?.id ? `User ${user.id}` : 'Current User')
}

async function getCurrentUserBaseReservations() {
  try {
    const { data } = await apiClient.get('/api/reservations/my')
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

function matchReservation(reservation, identity) {
  if (!reservation || !identity) {
    return false
  }

  const reservationId = toNumber(reservation.id)
  const identityId = toNumber(identity.id)

  if (reservationId && identityId && reservationId === identityId) {
    return true
  }

  return buildReservationKey(reservation) === identity.key
}

function cloneReservationWithStatus(reservation, status, extra = {}) {
  return {
    ...reservation,
    ...extra,
    status,
    normalizedStatus: normalizeReservationStatus(status)
  }
}

function createRequesterReservationRecord(request) {
  return {
    id: `swap-${request.id}`,
    roomId: request.target.roomId,
    roomName: request.target.roomName,
    seatId: request.target.seatId,
    seatCode: request.target.seatCode,
    reserveDate: request.target.reserveDate,
    startTime: request.target.startTime,
    endTime: request.target.endTime,
    userId: request.requester.id,
    username: request.requester.name,
    status: request.state === SWAP_REQUEST_STATE.REQUESTING
      ? RESERVATION_STATUS.REQUESTING
      : request.state === SWAP_REQUEST_STATE.APPROVED
        ? RESERVATION_STATUS.RESERVED
        : RESERVATION_STATUS.CANCELLED,
    normalizedStatus: request.state === SWAP_REQUEST_STATE.REQUESTING
      ? RESERVATION_STATUS.REQUESTING
      : request.state === SWAP_REQUEST_STATE.APPROVED
        ? RESERVATION_STATUS.RESERVED
        : RESERVATION_STATUS.CANCELLED,
    isVirtualSwapRecord: true,
    swapRequestId: request.id,
    swapRole: 'requester',
    swapState: request.state,
    swapMessage: request.message,
    createdAt: request.createdAt,
    updatedAt: request.updatedAt,
    sourceReservationId: request.target.reservationId ?? null
  }
}

function upsertReservationForList(list, reservation) {
  const index = list.findIndex((item) => String(item.id) === String(reservation.id))

  if (index === -1) {
    list.push(reservation)
    return
  }

  list.splice(index, 1, reservation)
}

export function listMockSwapRequests() {
  return getStoredSwapRequests()
}

export function decorateReservationsWithSwapState(reservations) {
  const currentUserId = getCurrentUserId()
  const list = Array.isArray(reservations)
    ? reservations.map((reservation) => ({
        ...reservation,
        normalizedStatus: normalizeReservationStatus(reservation.status),
        isVirtualSwapRecord: Boolean(reservation.isVirtualSwapRecord)
      }))
    : []

  if (!currentUserId) {
    return list
  }

  const requests = getStoredSwapRequests()

  for (const request of requests) {
    const isRequester = toNumber(request.requester?.id) === currentUserId
    const isHolder = toNumber(request.holder?.id) === currentUserId

    if (isHolder) {
      const matchedReservation = list.find((reservation) => matchReservation(reservation, request.target))

      if (matchedReservation) {
        const nextStatus = request.state === SWAP_REQUEST_STATE.REQUESTING
          ? RESERVATION_STATUS.PENDING
          : request.state === SWAP_REQUEST_STATE.APPROVED
            ? RESERVATION_STATUS.CANCELLED
            : RESERVATION_STATUS.RESERVED

        Object.assign(matchedReservation, cloneReservationWithStatus(matchedReservation, nextStatus, {
          swapRequestId: request.id,
          swapRole: 'holder',
          swapState: request.state,
          pendingRequesterName: request.requester?.name || `User ${request.requester?.id ?? ''}`,
          pendingMessage: request.message,
          pendingRequestedAt: request.createdAt
        }))
      }
    }

    if (isRequester) {
      upsertReservationForList(list, createRequesterReservationRecord(request))
    }
  }

  return list.sort((left, right) => {
    const leftDate = new Date(`${left.reserveDate || ''}T${formatTime(left.startTime) || '00:00'}:00`).getTime()
    const rightDate = new Date(`${right.reserveDate || ''}T${formatTime(right.startTime) || '00:00'}:00`).getTime()

    if (leftDate !== rightDate) {
      return rightDate - leftDate
    }

    return String(right.id).localeCompare(String(left.id))
  })
}

export async function checkMockSwapAvailability(payload) {
  const currentUserId = getCurrentUserId()
  const reservationIdentity = {
    id: payload.targetReservationId ?? payload.reservationId ?? null,
    key: buildReservationKey(payload)
  }

  const myReservations = await getCurrentUserBaseReservations()
  const ownReservation = myReservations.find(
    (reservation) =>
      isReservedStatus(reservation.status) &&
      buildReservationKey(reservation) === reservationIdentity.key
  )

  if (ownReservation || (payload.holderUserId && toNumber(payload.holderUserId) === currentUserId)) {
    return {
      canRequest: false,
      reason: 'OWN_SEAT'
    }
  }

  const requests = getStoredSwapRequests()
  const hasRejectedBefore = requests.some(
    (request) =>
      toNumber(request.requester?.id) === currentUserId &&
      request.state === SWAP_REQUEST_STATE.REJECTED &&
      request.target.key === reservationIdentity.key
  )

  if (hasRejectedBefore) {
    return {
      canRequest: false,
      reason: 'ALREADY_REJECTED'
    }
  }

  const hasActiveRequestInSameSlot = requests.some(
    (request) =>
      toNumber(request.requester?.id) === currentUserId &&
      request.state === SWAP_REQUEST_STATE.REQUESTING &&
      request.target.reserveDate === payload.reserveDate &&
      formatTime(request.target.startTime) === formatTime(payload.startTime) &&
      formatTime(request.target.endTime) === formatTime(payload.endTime)
  )

  if (hasActiveRequestInSameSlot) {
    return {
      canRequest: false,
      reason: 'HAS_ACTIVE_REQUEST'
    }
  }

  const seatLocked = requests.some(
    (request) => request.state === SWAP_REQUEST_STATE.REQUESTING && request.target.key === reservationIdentity.key
  )

  if (seatLocked) {
    return {
      canRequest: false,
      reason: 'SEAT_LOCKED'
    }
  }

  return {
    canRequest: true,
    reason: 'CAN_REQUEST'
  }
}

export async function createMockSwapRequest(payload) {
  const currentUserId = getCurrentUserId()

  if (!currentUserId) {
    throw new Error('Please sign in before sending a swap request.')
  }

  const availability = await checkMockSwapAvailability(payload.target)
  if (!availability.canRequest) {
    const error = new Error('This seat cannot receive a swap request right now.')
    error.code = availability.reason
    throw error
  }

  const requests = getStoredSwapRequests()
  const requestId = Date.now()
  const request = {
    id: requestId,
    state: SWAP_REQUEST_STATE.REQUESTING,
    message: payload.message?.trim() || 'Hi, I would like to request a seat swap. Thank you.',
    createdAt: nowIso(),
    updatedAt: nowIso(),
    requester: {
      id: currentUserId,
      name: payload.requesterName || getCurrentUserName()
    },
    holder: {
      id: payload.target.holderUserId ?? null,
      name: payload.target.holderName || ''
    },
    target: {
      reservationId: payload.target.targetReservationId ?? payload.target.reservationId ?? null,
      key: buildReservationKey(payload.target),
      roomId: payload.target.roomId,
      roomName: payload.target.roomName,
      seatId: payload.target.seatId,
      seatCode: payload.target.seatCode,
      reserveDate: payload.target.reserveDate,
      startTime: formatTime(payload.target.startTime),
      endTime: formatTime(payload.target.endTime)
    }
  }

  requests.push(request)
  saveStoredSwapRequests(requests)

  return {
    swapRequestId: requestId,
    status: RESERVATION_STATUS.REQUESTING
  }
}

function updateRequestState(requestId, nextState, expectedRole) {
  const currentUserId = getCurrentUserId()
  const requests = getStoredSwapRequests()
  const request = requests.find((item) => Number(item.id) === Number(requestId))

  if (!request) {
    throw new Error('Swap request not found.')
  }

  if (request.state !== SWAP_REQUEST_STATE.REQUESTING) {
    throw new Error('This swap request has already been processed.')
  }

  if (expectedRole === 'requester' && toNumber(request.requester?.id) !== currentUserId) {
    throw new Error('You can only withdraw your own swap requests.')
  }

  if (expectedRole === 'holder' && currentUserId && toNumber(request.holder?.id) && toNumber(request.holder?.id) !== currentUserId) {
    throw new Error('You can only manage swap requests sent to your reservation.')
  }

  request.state = nextState
  request.updatedAt = nowIso()
  saveStoredSwapRequests(requests)
  return request
}

export async function withdrawMockSwapRequest(requestId) {
  updateRequestState(requestId, SWAP_REQUEST_STATE.WITHDRAWN, 'requester')

  return {
    swapRequestId: Number(requestId),
    status: RESERVATION_STATUS.CANCELLED
  }
}

export async function approveMockSwapRequest(requestId) {
  updateRequestState(requestId, SWAP_REQUEST_STATE.APPROVED, 'holder')

  return {
    swapRequestId: Number(requestId),
    requesterStatus: RESERVATION_STATUS.RESERVED,
    holderStatus: RESERVATION_STATUS.CANCELLED
  }
}

export async function rejectMockSwapRequest(requestId) {
  updateRequestState(requestId, SWAP_REQUEST_STATE.REJECTED, 'holder')

  return {
    swapRequestId: Number(requestId),
    requesterStatus: RESERVATION_STATUS.CANCELLED,
    holderStatus: RESERVATION_STATUS.RESERVED
  }
}

export async function getMockPendingSwapRequest(payload) {
  const currentUserId = getCurrentUserId()
  const requests = getStoredSwapRequests()
  const reservationId = toNumber(payload.reservationId)
  const reservationKey = buildReservationKey(payload)

  const request = requests.find(
    (item) =>
      item.state === SWAP_REQUEST_STATE.REQUESTING &&
      toNumber(item.holder?.id) === currentUserId &&
      ((reservationId && toNumber(item.target.reservationId) === reservationId) || item.target.key === reservationKey)
  )

  if (!request) {
    return null
  }

  return {
    swapRequestId: request.id,
    requesterId: request.requester.id,
    requesterName: request.requester.name,
    message: request.message,
    createdAt: request.createdAt
  }
}
