<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import api from '../services/api.js'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')

const handleLogin = async () => {
  if (!email.value || !password.value) {
    alert('Email and password are required.')
    return
  }

  try {
    const response = await api.post('/api/auth/login', {
      email: email.value,
      password: password.value
    })

    authStore.setAuth(response.data.user)
    alert(response.data.message || 'Signed in successfully.')
    router.push({ name: 'dashboard' })
  } catch (error) {
    console.error('Login failed:', error)
    authStore.clearAuth()
    alert(`Sign-in failed: ${error.response?.data?.message || 'Incorrect email or password.'}`)
  }
}
</script>

<template>
  <div class="form-container">
    <form @submit.prevent="handleLogin">
      <h2>Sign In</h2>
      <div class="form-group">
        <label for="email">Email</label>
        <input type="email" id="email" v-model="email" placeholder="Enter your email" required>
      </div>
      <div class="form-group">
        <label for="password">Password</label>
        <input type="password" id="password" v-model="password" placeholder="Enter your password" required>
      </div>
      <button type="submit">Sign In</button>
    </form>
  </div>
</template>

<style scoped>
.form-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding-top: 50px;
}
form {
  width: 100%;
  max-width: 400px;
  padding: 2rem;
  border: 1px solid #ccc;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
h2 {
  text-align: center;
  margin-bottom: 1.5rem;
}
.form-group {
  margin-bottom: 1rem;
}
label {
  display: block;
  margin-bottom: 0.5rem;
}
input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}
button {
  width: 100%;
  padding: 0.75rem;
  background-color: hsla(160, 100%, 37%, 1);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}
button:hover {
  background-color: hsla(160, 100%, 30%, 1);
}
</style>
