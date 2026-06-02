<template>
  <span class="connection-indicator" :class="{ connected: store.connected }">
    {{ store.connected ? 'Connected' : 'Disconnected' }}
  </span>
</template>

<script setup lang="ts">
import { useReviewStore } from '../stores/reviewStore'

const store = useReviewStore()
</script>

<style scoped>
.connection-indicator {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background: #fee2e2;
  color: #dc2626;
}
.connection-indicator::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  background: #dc2626;
}
.connection-indicator.connected {
  background: #dcfce7;
  color: #16a34a;
}
.connection-indicator.connected::before {
  background: #16a34a;
}
</style>
