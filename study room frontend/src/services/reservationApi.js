import apiClient from '@/services/api'

export function getRooms() {
  return apiClient.get('/api/rooms')
}

export function getRoomSeats(roomId) {
  return apiClient.get(`/api/rooms/${roomId}/seats`)
}

export function getRoomTimeline(roomId, date) {
  return apiClient.get(`/api/rooms/${roomId}/timeline`, {
    params: { date }
  })
}

export function getSeatTimeline(seatId, date) {
  return apiClient.get(`/api/seats/${seatId}/timeline`, {
    params: { date }
  })
}

export function getRoomSeatStatus(roomId, payload) {
  return apiClient.post(`/api/reservations/seat-status/${roomId}`, payload)
}

export function createReservation(payload) {
  return apiClient.post('/api/reservations', payload)
}

export function getMyReservations() {
  return apiClient.get('/api/reservations/my')
}

export function updateReservation(reservationId, payload) {
  return apiClient.put(`/api/reservations/${reservationId}`, payload)
}

export function cancelReservation(reservationId) {
  return apiClient.delete(`/api/reservations/${reservationId}`)
}

export function getSeatComments(seatId) {
  return apiClient.get(`/api/seats/${seatId}/comments`)
}

export function createSeatComment(seatId, payload) {
  return apiClient.post(`/api/seats/${seatId}/comments`, payload)
}
