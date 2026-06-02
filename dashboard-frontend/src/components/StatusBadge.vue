<template>
  <span class="status-badge" :class="statusClass">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ReviewStatus } from '../types'

const props = defineProps<{ status: ReviewStatus }>()

const statusClass = computed(() => `status-${props.status.toLowerCase()}`)

const label = computed(() => {
  const labels: Record<ReviewStatus, string> = {
    QUEUED: 'Queued',
    CLONING_REPO: 'Cloning',
    FETCHING_DIFF: 'Fetching',
    ANALYZING: 'Analyzing',
    SANDBOXING: 'Sandbox',
    POSTING: 'Posting',
    COMPLETED: 'Done',
    FAILED: 'Failed'
  }
  return labels[props.status] || props.status
})
</script>

<style scoped>
.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}
.status-queued { background: #e2e8f0; color: #475569; }
.status-cloning_repo { background: #e0f2fe; color: #0369a1; }
.status-fetching_diff { background: #dbeafe; color: #1d4ed8; }
.status-analyzing { background: #fef3c7; color: #b45309; }
.status-sandboxing { background: #ede9fe; color: #7c3aed; }
.status-posting { background: #cffafe; color: #0891b2; }
.status-completed { background: #dcfce7; color: #16a34a; }
.status-failed { background: #fee2e2; color: #dc2626; }
</style>
