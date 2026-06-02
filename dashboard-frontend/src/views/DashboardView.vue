<template>
  <div class="dashboard-view">
    <div class="section">
      <h2>Reviews</h2>
      <ReviewList />
    </div>
    <div class="section">
      <ProgressTimeline />
    </div>
  </div>
</template>

<script setup lang="ts">
import ReviewList from '../components/ReviewList.vue'
import ProgressTimeline from '../components/ProgressTimeline.vue'
</script>

<style scoped>
.dashboard-view {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
.section h2 {
  font-size: 18px;
  margin-bottom: 12px;
  color: #1e293b;
}
</style>
