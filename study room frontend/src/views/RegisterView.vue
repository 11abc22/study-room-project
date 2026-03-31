<!-- src/views/RegisterView.vue -->
<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../services/api.js';

const router = useRouter();
const username = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');

const handleRegister = async () => {
    if (password.value !== confirmPassword.value) {
        alert('两次输入的密码不一致！');
        return;
    }

    try {
        console.log('即将发送注册请求...'); // 日志探针
        await api.post('/api/auth/register', {
            username: username.value,
            email: email.value,
            password: password.value
        });
        alert('注册成功！即将跳转到登录页面。');
        router.push({ name: 'login' });
    } catch (error) {
        console.error('注册失败，捕获到错误:', error);
        alert('注册失败: ' + (error.response?.data?.message || '请检查错误详情。'));
    }
}
</script>

<template>
  <div class="form-container">
    <form @submit.prevent="handleRegister">
      <h2>用户注册</h2>
      <div class="form-group">
        <label for="username">用户名</label>
        <input type="text" id="username" v-model="username" placeholder="请输入用户名" required>
      </div>
      <div class="form-group">
        <label for="email">邮箱</label>
        <input type="email" id="email" v-model="email" placeholder="请输入邮箱" required>
      </div>
      <div class="form-group">
        <label for="password">密码</label>
        <input type="password" id="password" v-model="password" placeholder="请输入密码" required>
      </div>
      <div class="form-group">
        <label for="confirmPassword">确认密码</label>
        <input type="password" id="confirmPassword" v-model="confirmPassword" placeholder="请再次输入密码" required>
      </div>
      <button type="submit">注册</button>
    </form>
  </div>
</template>

<style scoped>
/* 这里的样式与登录页面完全相同，你可以把它们提取到一个公共的CSS文件中，
   然后在两个组件中导入，以遵循 DRY (Don't Repeat Yourself) 原则。
   但为了本教程的简单性，我们暂时复制它。*/
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