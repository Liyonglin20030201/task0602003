<template>
  <div class="review-list">
    <table>
      <thead>
        <tr>
          <th>Repository</th>
          <th>PR</th>
          <th>Title</th>
          <th>Author</th>
          <th>Status</th>
          <th>Time</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="review in store.reviews" :key="review.id" @click="goToDetail(review.id)" class="clickable">
          <td>{{ review.repoFullName }}</td>
          <td>#{{ review.prNumber }}</td>
          <td>{{ review.prTitle }}</td>
          <td>{{ review.prAuthor }}</td>
          <td><StatusBadge :status="review.status" /></td>
          <td>{{ formatTime(review.createdAt) }}</td>
        </tr>
        <tr v-if="store.reviews.length === 0">
          <td colspan="6" class="empty">No reviews yet. Waiting for PR webhooks...</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useReviewStore } from '../stores/reviewStore'
import StatusBadge from './StatusBadge.vue'

const store = useReviewStore()
const router = useRouter()

onMounted(() => store.fetchReviews())

function goToDetail(id: string) {
  router.push({ name: 'review-detail', params: { id } })
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  return new Date(iso).toLocaleString()
}
</script>

<style scoped>
.review-list table {
  width: 100%;
  border-collapse: collapse;
}
.review-list th, .review-list td {
  padding: 10px 14px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}
.review-list th {
  background: #f8fafc;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  color: #64748b;
}
.clickable {
  cursor: pointer;
}
.clickable:hover {
  background: #f1f5f9;
}
.empty {
  text-align: center;
  color: #94a3b8;
  padding: 40px;
}
</style>
