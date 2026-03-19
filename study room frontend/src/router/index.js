import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/rooms',
      name: 'rooms',
      component: () => import('@/views/RoomsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/rooms/:id',
      name: 'room-detail',
      component: () => import('@/views/RoomDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my-reservations',
      name: 'my-reservations',
      component: () => import('@/views/MyReservationsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue')
    },
    {
      path: '/admin/reservations',
      name: 'AdminReservations',
      component: () => import('@/views/AdminReservationsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/comments',
      name: 'AdminComments',
      component: () => import('@/views/AdminCommentsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/notifications',
      name: 'AdminNotifications',
      component: () => import('@/views/AdminNotificationsView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const isAdmin = authStore.user?.username === 'admin'
  const adminRouteNames = new Set(['AdminReservations', 'AdminComments', 'AdminNotifications'])
  const bookingRouteNames = new Set(['rooms', 'room-detail', 'my-reservations'])

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' }
  }

  if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }

  if (isAdmin && bookingRouteNames.has(String(to.name))) {
    return { name: 'AdminReservations' }
  }

  if (!isAdmin && adminRouteNames.has(String(to.name))) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
