<template>
  <div class="review-detail" v-if="store.currentReview">
    <div class="detail-header">
      <router-link to="/" class="back-link">&larr; Back</router-link>
      <h2>{{ store.currentReview.prTitle }}</h2>
      <div class="meta">
        <span>{{ store.currentReview.repoFullName }}#{{ store.currentReview.prNumber }}</span>
        <span>by {{ store.currentReview.prAuthor }}</span>
        <StatusBadge :status="store.currentReview.status" />
      </div>
    </div>

    <div class="summary" v-if="store.currentReview.summary">
      <h3>Summary</h3>
      <p>{{ store.currentReview.summary }}</p>
    </div>

    <!-- Sandbox Execution Results Section -->
    <div class="sandbox-section" v-if="store.currentReview.sandboxExecutions?.length">
      <h3>
        Sandbox Execution Results
        <span class="sandbox-stats">
          {{ passedCount }}/{{ store.currentReview.sandboxExecutions.length }} passed
        </span>
      </h3>

      <div class="sandbox-list">
        <div
          v-for="exec in store.currentReview.sandboxExecutions"
          :key="exec.id"
          class="sandbox-card"
          :class="exec.executionStatus.toLowerCase()"
        >
          <div class="sandbox-card-header">
            <div class="sandbox-file-info">
              <span class="sandbox-status-icon">{{ statusIcon(exec.executionStatus) }}</span>
              <span class="sandbox-class-name">{{ exec.className }}</span>
              <span class="sandbox-file-path">{{ exec.filePath }}</span>
            </div>
            <div class="sandbox-meta">
              <span class="sandbox-status-label">{{ statusLabel(exec.executionStatus) }}</span>
              <span class="sandbox-time">{{ exec.executionTimeMs }}ms</span>
            </div>
          </div>

          <div class="sandbox-card-body">
            <!-- stdout output -->
            <div class="output-block" v-if="exec.stdout">
              <div class="output-label">stdout</div>
              <pre class="output-content stdout">{{ exec.stdout }}</pre>
            </div>

            <!-- stderr output -->
            <div class="output-block" v-if="exec.stderr">
              <div class="output-label">stderr</div>
              <pre class="output-content stderr">{{ exec.stderr }}</pre>
            </div>

            <!-- Extra info for failures -->
            <div class="sandbox-detail-row" v-if="exec.exitCode !== 0">
              <span class="detail-key">Exit Code:</span>
              <span class="detail-value">{{ exec.exitCode }}</span>
            </div>
            <div class="sandbox-detail-row" v-if="exec.timedOut">
              <span class="detail-key">Note:</span>
              <span class="detail-value timeout-note">Execution exceeded time limit</span>
            </div>
            <div class="sandbox-detail-row" v-if="exec.compilationFailed">
              <span class="detail-key">Note:</span>
              <span class="detail-value compile-note">Source code failed to compile</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI Review Comments Section -->
    <div class="comments-section" v-if="store.currentReview.comments?.length">
      <h3>Review Comments ({{ store.currentReview.comments.length }})</h3>
      <div v-for="comment in store.currentReview.comments" :key="comment.id" class="comment-card" :class="comment.severity">
        <div class="comment-header">
          <span class="severity">{{ comment.severity }}</span>
          <span class="location" v-if="comment.filePath">{{ comment.filePath }}:{{ comment.lineNumber }}</span>
        </div>
        <p class="comment-message">{{ comment.message }}</p>
        <p class="comment-suggestion" v-if="comment.suggestion">Suggestion: {{ comment.suggestion }}</p>
      </div>
    </div>

    <ProgressTimeline :review-id="store.currentReview.id" />
  </div>
  <div v-else class="loading">Loading...</div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useReviewStore } from '../stores/reviewStore'
import StatusBadge from './StatusBadge.vue'
import ProgressTimeline from './ProgressTimeline.vue'
import type { ExecutionStatus } from '../types'

const route = useRoute()
const store = useReviewStore()

onMounted(() => {
  store.fetchReview(route.params.id as string)
})

const passedCount = computed(() => {
  if (!store.currentReview?.sandboxExecutions) return 0
  return store.currentReview.sandboxExecutions.filter(e => e.executionStatus === 'SUCCESS').length
})

function statusIcon(status: ExecutionStatus): string {
  const icons: Record<ExecutionStatus, string> = {
    SUCCESS: 'O',
    COMPILATION_ERROR: 'X',
    RUNTIME_ERROR: 'X',
    TIMEOUT: '!',
    SANDBOX_ERROR: '?'
  }
  return icons[status] || '?'
}

function statusLabel(status: ExecutionStatus): string {
  const labels: Record<ExecutionStatus, string> = {
    SUCCESS: 'Passed',
    COMPILATION_ERROR: 'Compile Error',
    RUNTIME_ERROR: 'Runtime Error',
    TIMEOUT: 'Timed Out',
    SANDBOX_ERROR: 'Sandbox Error'
  }
  return labels[status] || status
}
</script>

<style scoped>
.detail-header {
  margin-bottom: 24px;
}
.back-link {
  color: #3b82f6;
  text-decoration: none;
  font-size: 14px;
}
.detail-header h2 {
  margin: 8px 0;
}
.meta {
  display: flex;
  gap: 16px;
  align-items: center;
  color: #64748b;
  font-size: 14px;
}
.summary {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}
.summary h3, .comments-section h3, .sandbox-section h3 {
  font-size: 14px;
  color: #475569;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* === Sandbox Section === */
.sandbox-section {
  margin-bottom: 24px;
}
.sandbox-stats {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
}
.sandbox-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sandbox-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}
.sandbox-card.success { border-left: 4px solid #16a34a; }
.sandbox-card.compilation_error { border-left: 4px solid #dc2626; }
.sandbox-card.runtime_error { border-left: 4px solid #ea580c; }
.sandbox-card.timeout { border-left: 4px solid #d97706; }
.sandbox-card.sandbox_error { border-left: 4px solid #7c3aed; }

.sandbox-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.sandbox-file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sandbox-status-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}
.success .sandbox-status-icon { background: #16a34a; }
.compilation_error .sandbox-status-icon { background: #dc2626; }
.runtime_error .sandbox-status-icon { background: #ea580c; }
.timeout .sandbox-status-icon { background: #d97706; }
.sandbox_error .sandbox-status-icon { background: #7c3aed; }

.sandbox-class-name {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
}
.sandbox-file-path {
  font-size: 12px;
  color: #94a3b8;
  font-family: monospace;
}
.sandbox-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sandbox-status-label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}
.success .sandbox-status-label { color: #16a34a; }
.compilation_error .sandbox-status-label { color: #dc2626; }
.runtime_error .sandbox-status-label { color: #ea580c; }
.timeout .sandbox-status-label { color: #d97706; }
.sandbox_error .sandbox-status-label { color: #7c3aed; }

.sandbox-time {
  font-size: 12px;
  color: #64748b;
  font-family: monospace;
}

.sandbox-card-body {
  padding: 12px 14px;
}
.output-block {
  margin-bottom: 10px;
}
.output-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: #94a3b8;
  margin-bottom: 4px;
}
.output-content {
  margin: 0;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}
.output-content.stdout {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}
.output-content.stderr {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}
.sandbox-detail-row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  margin-top: 6px;
}
.detail-key {
  color: #64748b;
  font-weight: 500;
}
.detail-value {
  color: #1e293b;
}
.timeout-note {
  color: #d97706;
  font-weight: 500;
}
.compile-note {
  color: #dc2626;
  font-weight: 500;
}

/* === Comments Section === */
.comments-section {
  margin-bottom: 24px;
}
.comment-card {
  padding: 12px 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  border-left: 4px solid #e2e8f0;
}
.comment-card.error { border-left-color: #dc2626; background: #fef2f2; }
.comment-card.warning { border-left-color: #f59e0b; background: #fffbeb; }
.comment-card.suggestion { border-left-color: #3b82f6; background: #eff6ff; }
.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
}
.severity {
  font-weight: 700;
  text-transform: uppercase;
}
.location {
  color: #64748b;
  font-family: monospace;
}
.comment-message {
  margin: 0;
  font-size: 14px;
}
.comment-suggestion {
  margin: 8px 0 0;
  font-size: 13px;
  color: #475569;
  font-style: italic;
}
.loading {
  text-align: center;
  color: #94a3b8;
  padding: 40px;
}
</style>
