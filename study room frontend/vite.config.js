import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 这一段是核心，它告诉 Vite, '@' 就等于 'src' 目录
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})