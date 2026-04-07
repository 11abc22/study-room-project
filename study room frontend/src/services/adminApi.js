import apiClient from '@/services/api'

export function getAllReservations(params) {
  return apiClient.get('/api/admin/reservations', { params })
}

export function deleteReservation(id) {
  return apiClient.delete(`/api/admin/reservations/${id}`)
}

export function updateReservationStatus(id, status) {
  return apiClient.put(`/api/admin/reservations/${id}/status`, { status })
}

export function getAllComments() {
  return apiClient.get('/api/admin/comments')
}

export function deleteComment(id) {
  return apiClient.delete(`/api/admin/comments/${id}`)
}

export function sendAdminTestEmail() {
  return apiClient.post('/api/admin/notifications/test-email')
}
