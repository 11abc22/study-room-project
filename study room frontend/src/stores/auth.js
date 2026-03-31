import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

function parseStoredUser() {
  const rawUser = localStorage.getItem('user')

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser)
  } catch (error) {
    console.warn('Failed to parse stored user:', error)
    localStorage.removeItem('user')
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(parseStoredUser())

  const isAuthenticated = computed(() => !!user.value?.id)

  function setUser(newUser) {
    user.value = newUser
  }

  function setAuth(authUser) {
    user.value = authUser
  }

  function clearAuth() {
    user.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  watch(
    user,
    (newUser) => {
      if (newUser) {
        localStorage.setItem('user', JSON.stringify(newUser))
      } else {
        localStorage.removeItem('user')
      }
    },
    { deep: true }
  )

  return {
    user,
    isAuthenticated,
    setUser,
    setAuth,
    clearAuth
  }
})
