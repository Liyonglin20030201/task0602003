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
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useReviewStore } from '../stores/reviewStore'
import StatusBadge from './StatusBadge.vue'
import ProgressTimeline from './ProgressTimeline.vue'

const route = useRoute()
const store = useReviewStore()

onMounted(() => {
  store.fetchReview(route.params.id as string)
})
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
.summary h3, .comments-section h3 {
  font-size: 14px;
  color: #475569;
  margin-bottom: 8px;
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
