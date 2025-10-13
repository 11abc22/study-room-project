<!-- src/views/LoginView.vue (最终版本) -->

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

// --- 1. 导入 Store 和 API ---
// 使用相对路径！
import { useAuthStore } from '../stores/auth.js'
import api from '../services/api.js'

const router = useRouter()
const authStore = useAuthStore() // 获取 store 实例

const email = ref('')
const password = ref('')

// --- 2. 升级 handleLogin 函数 ---
const handleLogin = async () => {
  // 基本校验
  if (!email.value || !password.value) {
    alert('邮箱和密码不能为空！')
    return
  }

  try {
    console.log('即将发送登录请求...');
    const response = await api.post('/api/auth/login', {
      email: email.value,
      password: password.value
    })

    // --- 3. 处理成功响应 ---
    // 从响应中获取 token
    const token = response.data.token

    // 调用 store 的 action 来保存 token
    authStore.setToken(token)

    alert('登录成功！')
    // 跳转到受保护的仪表盘页面
    router.push({ name: 'dashboard' })

  } catch (error) {
    // --- 4. 处理失败响应 ---
    console.error('登录失败，捕获到错误:', error);
    authStore.clearAuth() // 登录失败时，清除可能存在的旧 token
    alert('登录失败: ' + (error.response?.data?.message || '用户名或密码错误。'));
  }
}
</script>


<template>
  <!-- template 部分保持完全不变 -->
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
  /* style 部分也保持完全不变 */
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