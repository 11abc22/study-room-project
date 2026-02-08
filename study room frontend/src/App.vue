<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const isAuthPage = computed(() => route.name === 'login' || route.name === 'register')
const isAdmin = computed(() => authStore.user?.username === 'admin')
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <RouterLink to="/dashboard">Study Room Reservation System</RouterLink>
      </div>
      <nav class="nav-links">
        <template v-if="authStore.isAuthenticated">
          <template v-if="isAdmin">
            <RouterLink to="/dashboard">Home</RouterLink>
            <RouterLink to="/admin/reservations">Reservation Management</RouterLink>
            <RouterLink to="/admin/comments">Comment Management</RouterLink>
          </template>
          <template v-else>
            <RouterLink to="/dashboard">Home</RouterLink>
            <RouterLink to="/rooms">Study Rooms</RouterLink>
            <RouterLink to="/my-reservations">My Reservations</RouterLink>
          </template>
        </template>
        <template v-else>
          <RouterLink to="/login">Sign In</RouterLink>
          <RouterLink to="/register">Sign Up</RouterLink>
        </template>
      </nav>
    </header>

    <main :class="['page-container', { compact: isAuthPage }]">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f5f7fb;
  color: #1f2937;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.brand a {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  text-decoration: none;
}

.nav-links {
  display: flex;
  gap: 16px;
}

.nav-links a {
  color: #4b5563;
  text-decoration: none;
  font-weight: 500;
}

.nav-links a.router-link-active {
  color: #2563eb;
}

.page-container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-container.compact {
  max-width: 520px;
}
</style>
