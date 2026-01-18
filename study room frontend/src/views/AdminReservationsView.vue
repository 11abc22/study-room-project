<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAllReservations, deleteReservation, updateReservationStatus } from '@/services/adminApi'
import { getReservationStatusMeta, isReservedStatus } from '@/constants/reservationStatus'

const router = useRouter()

const reservations = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const cancellingId = ref(null)
const updatingId = ref(null)

const filterUserId = ref('')
const filterStatus = ref('')
const filterDate = ref('')

async function loadReservations() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  const params = {}
  if (filterUserId.value) params.userId = filterUserId.value
  if (filterStatus.value !== '') params.status = filterStatus.value
  if (filterDate.value) params.date = filterDate.value

  try {
    const { data } = await getAllReservations(params)
    reservations.value = data
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to load reservations. Please try again later.'
  } finally {
    loading.value = false
  }
}

function formatTime(time) {
  if (!time) return ''
  return String(time).slice(0, 5)
}

function statusText(status) {
  return getReservationStatusMeta(status).label
}

function statusClass(status) {
  return getReservationStatusMeta(status).className
}

async function handleCancel(id) {
  successMessage.value = ''
  errorMessage.value = ''
  cancellingId.value = id

  try {
    const { data } = await deleteReservation(id)
    successMessage.value = data.message || 'Reservation cancelled.'
    await loadReservations()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to cancel the reservation. Please try again later.'
  } finally {
    cancellingId.value = null
  }
}

function parseStatusValue(value) {
  if (value === '') {
    return ''
  }

  const numericValue = Number(value)
  return Number.isNaN(numericValue) ? value : numericValue
}

async function handleUpdateStatus(id, newStatus) {
  successMessage.value = ''
  errorMessage.value = ''
  updatingId.value = id

  try {
    const { data } = await updateReservationStatus(id, newStatus)
    successMessage.value = data.message || 'Status updated.'
    await loadReservations()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to update the status. Please try again later.'
  } finally {
    updatingId.value = null
  }
}

function goBack() {
  router.push({ name: 'dashboard' })
}
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Admin Panel</p>
        <h1>Reservation Management</h1>
        <p>Review all reservations, apply filters, and update booking status.</p>
      </div>
      <button class="ghost-button" @click="goBack">Back to Home</button>
    </header>

    <div class="panel">
      <h3>Filters</h3>
      <div class="filter-grid">
        <label>
          <span>User ID</span>
          <input v-model="filterUserId" type="number" placeholder="Enter user ID" />
        </label>
        <label>
          <span>Status</span>
          <select v-model="filterStatus">
            <option value="">All</option>
            <option value="1">Reserved</option>
            <option value="0">Cancelled</option>
            <option value="2">Cancelled</option>
            <option value="REQUESTING">Requesting</option>
            <option value="PENDING">Pending</option>
          </select>
        </label>
        <label>
          <span>Date</span>
          <input v-model="filterDate" type="date" />
        </label>
      </div>
      <div class="actions">
        <button class="primary-button" @click="loadReservations">Search</button>
        <button class="ghost-button" @click="filterUserId = ''; filterStatus = ''; filterDate = ''; loadReservations()">Reset</button>
      </div>
    </div>

    <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>
    <div v-if="loading" class="feedback">Loading reservations...</div>
    <div v-else-if="!reservations.length" class="feedback">No reservations match the current filters.</div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>Study Room</th>
            <th>Seat</th>
            <th>Date</th>
            <th>Time</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in reservations" :key="r.id">
            <td>{{ r.id }}</td>
            <td>
              <span class="user-info">{{ r.username || `User ${r.userId}` }}</span>
              <span class="user-id">(ID: {{ r.userId }})</span>
            </td>
            <td>{{ r.roomName || 'Unknown' }}</td>
            <td>{{ r.seatCode || 'Unknown' }}</td>
            <td>{{ r.reserveDate }}</td>
            <td>{{ formatTime(r.startTime) }} - {{ formatTime(r.endTime) }}</td>
            <td><span :class="['status', statusClass(r.status)]">{{ statusText(r.status) }}</span></td>
            <td>
              <div class="action-group">
                <button
                  v-if="isReservedStatus(r.status)"
                  class="danger-button small"
                  :disabled="cancellingId === r.id"
                  @click="handleCancel(r.id)"
                >
                  {{ cancellingId === r.id ? 'Cancelling...' : 'Cancel Reservation' }}
                </button>
                <select
                  v-else
                  class="status-select"
                  :disabled="updatingId === r.id"
                  @change="handleUpdateStatus(r.id, parseStatusValue($event.target.value)); $event.target.value = ''"
                >
                  <option value="">Change status</option>
                  <option value="1">Set to Reserved</option>
                  <option value="0">Set to Cancelled</option>
                  <option value="2">Set to Cancelled</option>
                  <option value="REQUESTING">Set to Requesting</option>
                  <option value="PENDING">Set to Pending</option>
                </select>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav class="admin-nav">
      <router-link :to="{ name: 'AdminComments' }" class="nav-link">Manage Comments →</router-link>
    </nav>
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.panel,
.feedback,
.table-wrapper {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

h1, h2, h3, p {
  margin-top: 0;
}

.panel h3 {
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #374151;
  font-weight: 500;
  font-size: 14px;
}

input, select {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.primary-button,
.ghost-button,
.danger-button {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.primary-button {
  background: #2563eb;
  color: #fff;
}

.ghost-button {
  background: #eff6ff;
  color: #1d4ed8;
}

.danger-button {
  background: #fee2e2;
  color: #b91c1c;
}

.danger-button.small {
  padding: 6px 12px;
  font-size: 13px;
}

.danger-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.feedback {
  color: #374151;
}

.feedback.error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.feedback.success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.table-wrapper {
  overflow-x: auto;
  padding: 0;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
  white-space: nowrap;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  position: sticky;
  top: 0;
}

.user-info {
  font-weight: 500;
}

.user-id {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 4px;
}

.status {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
}

.status.reserved {
  background: #dcfce7;
  color: #166534;
}

.status.cancelled {
  background: #f3f4f6;
  color: #4b5563;
}

.status.requesting {
  background: #fef3c7;
  color: #b45309;
}

.status.pending {
  background: #dbeafe;
  color: #1d4ed8;
}

.action-group {
  display: flex;
  gap: 8px;
}

.status-select {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 6px 10px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.admin-nav {
  display: flex;
  justify-content: flex-end;
}

.nav-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.nav-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .data-table th,
  .data-table td {
    padding: 8px 10px;
    font-size: 13px;
  }
}
</style>
