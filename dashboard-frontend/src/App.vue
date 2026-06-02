<template>
  <div id="app">
    <header class="app-header">
      <h1>PR Review Bot</h1>
      <ConnectionIndicator />
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import ConnectionIndicator from './components/ConnectionIndicator.vue'
import { useWebSocket } from './composables/useWebSocket'

useWebSocket()
</script>
