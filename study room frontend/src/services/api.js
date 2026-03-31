import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    const userId = authStore.user?.id

    if (userId) {
      config.headers['X-User-Id'] = String(userId)
    } else if (config.headers['X-User-Id']) {
      delete config.headers['X-User-Id']
    }

    if (config.headers.Authorization) {
      delete config.headers.Authorization
    }

    return config
  },
  (error) => Promise.reject(error)
)

export default apiClient
