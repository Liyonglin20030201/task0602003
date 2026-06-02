<template>
  <div class="progress-timeline" v-if="messages.length > 0">
    <h3>Live Progress</h3>
    <div class="timeline">
      <div v-for="msg in messages" :key="msg.reviewId + msg.timestamp" class="timeline-item">
        <StatusBadge :status="msg.status" />
        <span class="detail">{{ msg.details }}</span>
        <span class="time">{{ formatTime(msg.timestamp) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useReviewStore } from '../stores/reviewStore'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{ reviewId?: string }>()
const store = useReviewStore()

const messages = computed(() => {
  if (props.reviewId) {
    return store.progressMessages.filter(m => m.reviewId === props.reviewId)
  }
  return store.progressMessages.slice(0, 20)
})

function formatTime(iso: string): string {
  if (!iso) return ''
  return new Date(iso).toLocaleTimeString()
}
</script>

<style scoped>
.progress-timeline h3 {
  margin-bottom: 12px;
  font-size: 14px;
  color: #475569;
}
.timeline {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.timeline-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}
.detail {
  flex: 1;
  color: #334155;
}
.time {
  color: #94a3b8;
  font-size: 12px;
}
</style>
