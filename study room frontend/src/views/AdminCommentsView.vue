<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAllComments, deleteComment } from '@/services/adminApi'

const router = useRouter()

const comments = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const deletingId = ref(null)

async function loadComments() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const { data } = await getAllComments()
    comments.value = data
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '加载留言列表失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function formatTime(datetime) {
  if (!datetime) return ''
  return String(datetime).replace('T', ' ').slice(0, 16)
}

async function handleDelete(id) {
  successMessage.value = ''
  errorMessage.value = ''
  deletingId.value = id

  try {
    const { data } = await deleteComment(id)
    successMessage.value = data.message || '留言已删除'
    await loadComments()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '删除留言失败，请稍后重试。'
  } finally {
    deletingId.value = null
  }
}

function goBack() {
  router.push({ name: 'dashboard' })
}

onMounted(() => {
  loadComments()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>留言管理</h1>
        <p>查看和删除所有座位留言。</p>
      </div>
      <button class="ghost-button" @click="goBack">返回首页</button>
    </header>

    <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>
    <div v-if="loading" class="feedback">正在加载留言列表...</div>
    <div v-else-if="!comments.length" class="feedback">暂无留言记录。</div>

    <div v-else class="comment-list">
      <article v-for="comment in comments" :key="comment.id" class="comment-card">
        <div class="comment-header">
          <div>
            <h3>
              <span class="username">{{ comment.username || '用户' + comment.userId }}</span>
              <span class="user-id">（ID: {{ comment.userId }}）</span>
            </h3>
            <p class="meta">
              座位：{{ comment.seatCode || '未知' }}（ID: {{ comment.seatId }}）· {{ formatTime(comment.createdAt) }}
            </p>
          </div>
          <button
            class="danger-button small"
            :disabled="deletingId === comment.id"
            @click="handleDelete(comment.id)"
          >
            {{ deletingId === comment.id ? '删除中...' : '删除留言' }}
          </button>
        </div>
        <p class="comment-content">{{ comment.content }}</p>
      </article>
    </div>

    <nav class="admin-nav">
      <router-link :to="{ name: 'AdminReservations' }" class="nav-link">← 管理预约</router-link>
    </nav>
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.feedback,
.comment-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

h1, h3, p {
  margin-top: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.username {
  font-weight: 600;
  color: #111827;
}

.user-id {
  color: #9ca3af;
  font-size: 13px;
}

.meta {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.comment-content {
  margin: 12px 0 0;
  color: #374151;
  line-height: 1.6;
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 10px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.danger-button,
.ghost-button {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.danger-button {
  background: #fee2e2;
  color: #b91c1c;
}

.danger-button.small {
  padding: 6px 12px;
  font-size: 13px;
  white-space: nowrap;
}

.danger-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.ghost-button {
  background: #eff6ff;
  color: #1d4ed8;
}

.feedback {
  color: #374151;
}

.feedback.error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.feedback.success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.admin-nav {
  display: flex;
  justify-content: flex-start;
}

.nav-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.nav-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .comment-header {
    flex-direction: column;
  }
}
</style>
