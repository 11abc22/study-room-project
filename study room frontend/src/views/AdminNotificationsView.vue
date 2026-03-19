<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getEmailTemplates,
  resetEmailTemplate,
  sendAdminTestEmail,
  updateEmailTemplate
} from '@/services/adminApi'

const router = useRouter()

const templates = ref([])
const placeholders = ref({})
const selectedTemplateKey = ref('')
const formSubject = ref('')
const formBody = ref('')
const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const sendingTestEmail = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const templateDescriptions = {
  SWAP_REQUEST_CREATED: 'User A initiates a swap request and the email is sent to User B.',
  SWAP_REQUEST_APPROVED: 'User B approves the request and the email is sent to User A.',
  SWAP_REQUEST_REJECTED: 'User B rejects the request and the email is sent to User A.',
  SWAP_REQUEST_EXPIRED: 'The request expires and the email is sent to User A.',
  ADMIN_TEST: 'Admin test email used to verify that the mail delivery system is working.'
}

const selectedTemplate = computed(() =>
  templates.value.find((template) => template.templateKey === selectedTemplateKey.value) || null
)

const placeholderEntries = computed(() => Object.entries(placeholders.value || {}))
const hasSelection = computed(() => Boolean(selectedTemplate.value))

function syncFormWithSelection() {
  formSubject.value = selectedTemplate.value?.subject || ''
  formBody.value = selectedTemplate.value?.body || ''
}

function selectTemplate(templateKey) {
  selectedTemplateKey.value = templateKey
  syncFormWithSelection()
  successMessage.value = ''
  errorMessage.value = ''
}

async function loadTemplates() {
  loading.value = true
  errorMessage.value = ''

  try {
    const { data } = await getEmailTemplates()
    templates.value = data.templates || []
    placeholders.value = data.placeholders || {}

    if (!selectedTemplateKey.value && templates.value.length) {
      selectedTemplateKey.value = templates.value[0].templateKey
    }

    if (selectedTemplateKey.value && !templates.value.some((item) => item.templateKey === selectedTemplateKey.value)) {
      selectedTemplateKey.value = templates.value[0]?.templateKey || ''
    }

    syncFormWithSelection()
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to load email templates. Please try again later.'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!selectedTemplate.value) return

  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const { data } = await updateEmailTemplate(selectedTemplate.value.templateKey, {
      subject: formSubject.value,
      body: formBody.value
    })

    templates.value = templates.value.map((template) =>
      template.templateKey === data.templateKey ? data : template
    )
    syncFormWithSelection()
    successMessage.value = 'Template saved successfully.'
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to save the template. Please try again later.'
  } finally {
    saving.value = false
  }
}

async function handleReset() {
  if (!selectedTemplate.value) return

  resetting.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const { data } = await resetEmailTemplate(selectedTemplate.value.templateKey)
    templates.value = templates.value.map((template) =>
      template.templateKey === data.templateKey ? data : template
    )
    syncFormWithSelection()
    successMessage.value = 'Template restored to the default content.'
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to restore the default template. Please try again later.'
  } finally {
    resetting.value = false
  }
}

async function handleSendTestEmail() {
  sendingTestEmail.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const { data } = await sendAdminTestEmail()
    successMessage.value = data.message || `Test email sent to ${data.targetEmail}.`
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Failed to send the test email. Please try again later.'
  } finally {
    sendingTestEmail.value = false
  }
}

function goBack() {
  router.push({ name: 'dashboard' })
}

onMounted(() => {
  loadTemplates()
})
</script>

<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Admin Panel</p>
        <h1>Notification Templates</h1>
        <p>Manage email templates for swap request notifications and verify mail delivery from one place.</p>
      </div>
      <button class="ghost-button" @click="goBack">Back to Home</button>
    </header>

    <nav class="admin-nav-tabs">
      <router-link :to="{ name: 'AdminReservations' }" class="nav-tab">Reservations</router-link>
      <router-link :to="{ name: 'AdminComments' }" class="nav-tab">Comments</router-link>
      <router-link :to="{ name: 'AdminNotifications' }" class="nav-tab active">Notifications</router-link>
    </nav>

    <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="feedback success">{{ successMessage }}</div>

    <div class="layout">
      <aside class="panel sidebar">
        <div class="panel-header">
          <h2>Templates</h2>
          <button class="ghost-button small" :disabled="loading" @click="loadTemplates">
            {{ loading ? 'Refreshing...' : 'Refresh' }}
          </button>
        </div>

        <div v-if="loading" class="empty-state">Loading templates...</div>
        <div v-else-if="!templates.length" class="empty-state">No templates available.</div>
        <div v-else class="template-list">
          <button
            v-for="template in templates"
            :key="template.templateKey"
            :class="['template-item', { active: selectedTemplateKey === template.templateKey }]"
            @click="selectTemplate(template.templateKey)"
          >
            <strong>{{ template.templateKey }}</strong>
            <span>{{ templateDescriptions[template.templateKey] || 'Email template' }}</span>
          </button>
        </div>
      </aside>

      <div class="content-column">
        <section class="panel">
          <div class="panel-header">
            <div>
              <h2>Template Editor</h2>
              <p v-if="selectedTemplate">{{ templateDescriptions[selectedTemplate.templateKey] }}</p>
            </div>
            <div class="header-actions">
              <button class="primary-button" :disabled="!hasSelection || saving" @click="handleSave">
                {{ saving ? 'Saving...' : 'Save Template' }}
              </button>
              <button class="danger-button" :disabled="!hasSelection || resetting" @click="handleReset">
                {{ resetting ? 'Restoring...' : 'Restore Default' }}
              </button>
            </div>
          </div>

          <div v-if="!hasSelection" class="empty-state">Select a template to start editing.</div>
          <div v-else class="editor-form">
            <label>
              <span>Template Key</span>
              <input :value="selectedTemplate.templateKey" type="text" disabled />
            </label>
            <label>
              <span>Subject</span>
              <input v-model="formSubject" type="text" placeholder="Enter the email subject" />
            </label>
            <label>
              <span>Body</span>
              <textarea
                v-model="formBody"
                rows="16"
                placeholder="Enter the email body. Use placeholders like {{requesterUsername}}."
              />
            </label>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div>
              <h2>Available Placeholders</h2>
              <p>These placeholders can be used in the template subject and body.</p>
            </div>
          </div>
          <div class="placeholder-grid">
            <div v-for="[key, description] in placeholderEntries" :key="key" class="placeholder-card">
              <code>{{ key }}</code>
              <p>{{ description }}</p>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div>
              <h2>Mail Delivery Test</h2>
              <p>Send a test email to the current admin account using the ADMIN_TEST template.</p>
            </div>
            <button class="primary-button" :disabled="sendingTestEmail" @click="handleSendTestEmail">
              {{ sendingTestEmail ? 'Sending Test Email...' : 'Send Test Email to Me' }}
            </button>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.panel,
.feedback {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 24px;
}

.page-header,
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 600;
}

h1, h2, p {
  margin-top: 0;
}

.layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
}

.content-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar {
  align-self: start;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.template-item {
  width: 100%;
  text-align: left;
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  color: #1f2937;
}

.template-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.template-item span {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #374151;
  font-weight: 500;
  font-size: 14px;
}

input,
textarea {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}

textarea {
  resize: vertical;
  min-height: 280px;
}

.placeholder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.placeholder-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
  background: #f9fafb;
}

.placeholder-card code {
  display: inline-block;
  margin-bottom: 8px;
  color: #1d4ed8;
  font-weight: 700;
}

.placeholder-card p,
.empty-state {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.admin-nav-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.nav-tab {
  padding: 10px 16px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  text-decoration: none;
  font-weight: 600;
}

.nav-tab.active {
  background: #2563eb;
  color: #fff;
}

.primary-button,
.ghost-button,
.danger-button {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.ghost-button {
  background: #eff6ff;
  color: #1d4ed8;
}

.ghost-button.small {
  padding: 8px 12px;
}

.primary-button {
  background: #2563eb;
  color: #fff;
}

.danger-button {
  background: #fee2e2;
  color: #b91c1c;
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

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header,
  .panel-header {
    flex-direction: column;
  }
}
</style>
