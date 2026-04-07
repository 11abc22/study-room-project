<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const displayName = computed(() => authStore.user?.username || authStore.user?.email || 'Guest')
const isAdmin = computed(() => authStore.user?.username === 'admin')
const todaySummary = computed(() => {
  if (isAdmin.value) {
    return [
      { title: 'Reservation Management', description: 'Admin: review other users\' reservations, filter records, cancel bookings, and update status.', action: 'Open Admin Panel', routeName: 'AdminReservations' },
      { title: 'Comment Management', description: 'Admin: review and remove all seat comments.', action: 'Open Admin Panel', routeName: 'AdminComments' },
      { title: 'Notification Management', description: 'Admin: edit email templates for swap notifications and send yourself a test email.', action: 'Open Admin Panel', routeName: 'AdminNotifications' }
    ]
  }

  return [
    { title: 'Study Rooms', description: 'Browse available study rooms, locations, and room details.', action: 'Open Rooms', routeName: 'rooms' },
    { title: 'My Reservations', description: 'Review your current reservations and manage upcoming bookings.', action: 'View Reservations', routeName: 'my-reservations' }
  ]
})

const handleLogout = () => {
  authStore.clearAuth()
  router.push({ name: 'login' })
}

const goTo = (routeName) => {
  router.push({ name: routeName })
}
</script>

<template>
  <section class="dashboard">
    <div class="hero-card">
      <div>
        <p class="eyebrow">Quick Access</p>
        <h1>Welcome back, {{ displayName }}</h1>
        <p class="hero-text">
          {{ isAdmin ? 'Manage reservations, comments, and notification templates from one place.' : 'Access room browsing, reservations, and admin tools from one place.' }}
        </p>
      </div>
      <button class="logout-button" @click="handleLogout">Sign Out</button>
    </div>

    <div class="entry-grid">
      <article v-for="item in todaySummary" :key="item.routeName" class="entry-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <button @click="goTo(item.routeName)">{{ item.action }}</button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-card,
.entry-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

h1,
h2,
p {
  margin-top: 0;
}

.hero-text,
.entry-card p {
  color: #4b5563;
  line-height: 1.6;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

button {
  padding: 10px 16px;
  border: none;
  border-radius: 10px;
  background: #2563eb;
  color: #ffffff;
  cursor: pointer;
}

.logout-button {
  background: #ef4444;
}
</style>
