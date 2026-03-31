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
    alert('邮箱和密码不能为空！')
    return
  }

  try {
    const response = await api.post('/api/auth/login', {
      email: email.value,
      password: password.value
    })

    authStore.setAuth(response.data.user)

    alert(response.data.message || '登录成功！')
    router.push({ name: 'dashboard' })
  } catch (error) {
    console.error('登录失败，捕获到错误:', error)
    authStore.clearAuth()
    alert('登录失败: ' + (error.response?.data?.message || '用户名或密码错误。'))
  }
}
</script>

<template>
  <div class="form-container">
    <form @submit.prevent="handleLogin">
      <h2>用户登录</h2>
      <div class="form-group">
        <label for="email">邮箱</label>
        <input type="email" id="email" v-model="email" placeholder="请输入您的邮箱" required>
      </div>
      <div class="form-group">
        <label for="password">密码</label>
        <input type="password" id="password" v-model="password" placeholder="请输入您的密码" required>
      </div>
      <button type="submit">登录</button>
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
