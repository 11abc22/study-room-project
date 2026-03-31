a<!-- src/App.vue -->
<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header>
    <div class="wrapper">
      <nav>
        <!-- 我们将默认的 Home 和 About 链接替换为登录和注册 -->
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
      </nav>
    </div>
  </header>

  <!-- RouterView 会根据当前 URL 渲染对应的页面组件 -->
  <RouterView />
</template>

<style scoped>
/* 这里可以保留或修改默认样式，让导航栏更好看 */
header {
  line-height: 1.5;
  max-height: 100vh;
}

nav {
  width: 100%;
  font-size: 1rem;
  text-align: center;
  margin-top: 2rem;
}

nav a {
  display: inline-block;
  padding: 0 1rem;
  border-left: 1px solid var(--color-border);
}

nav a:first-of-type {
  border: 0;
}
</style>