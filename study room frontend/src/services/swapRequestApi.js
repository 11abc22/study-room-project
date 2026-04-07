import apiClient from '@/services/api'

export function decorateMyReservations(reservations) {
  if (!Array.isArray(reservations)) {
    return []
  }

  return reservations.map((reservation) => ({
    ...reservation,
    status: reservation.displayStatus || reservation.status,
    isVirtualSwapRecord: Boolean(reservation.virtual || reservation.isVirtualSwapRecord)
  }))
}

export function createSwapRequest(payload) {
  return apiClient.post('/api/swap-requests', payload)
}

export function withdrawSwapRequest(id) {
  return apiClient.put(`/api/swap-requests/${id}/withdraw`)
}

export function approveSwapRequest(id) {
  return apiClient.put(`/api/swap-requests/${id}/approve`)
}

export function rejectSwapRequest(id) {
  return apiClient.put(`/api/swap-requests/${id}/reject`)
}

export function checkSwapRequestAvailability(params) {
  return apiClient.get('/api/swap-requests/check', { params })
}

export function getMyPendingSwapRequest(params) {
  return apiClient.get('/api/swap-requests/my-pending', { params })
}
