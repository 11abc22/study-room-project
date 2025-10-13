// src/stores/auth.js (持久化版本)

import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue' // 1. 导入 watch

export const useAuthStore = defineStore('auth', () => {
  // --- State ---
  // 2. 初始化时，尝试从 localStorage 读取 token
  const token = ref(localStorage.getItem('token'))
  const user = ref(JSON.parse(localStorage.getItem('user'))) // user 信息通常存为 JSON 字符串

  // --- Getters ---
  const isAuthenticated = computed(() => !!token.value)

  // --- Actions ---
  function setToken(newToken) {
    token.value = newToken
  }

  function setUser(newUser) {
    user.value = newUser
  }


  function clearAuth() {
    token.value = null
    user.value = null
    // 4. 清除时也要从 localStorage 中移除
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // --- 持久化逻辑 ---
  // 3. 使用 watch 监听 token 的变化，一旦变化就存入 localStorage
  watch(token, (newToken) => {
    if (newToken) {
      localStorage.setItem('token', newToken)
    } else {
      localStorage.removeItem('token')
    }
  })

  watch(user, (newUser) => { // 同样监听 user 的变化
    if (newUser) {
      localStorage.setItem('user', JSON.stringify(newUser))
    } else {
      localStorage.removeItem('user')
    }
  }, { deep: true }) // deep: true 确保对象内部属性变化也能被监听到


  return {
    token,
    user,
    isAuthenticated,
    setToken,
    setUser,
    clearAuth
  }
})