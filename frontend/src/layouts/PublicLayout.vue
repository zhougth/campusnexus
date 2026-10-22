<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <div class="public-layout">
    <AppHeader />
    <main class="public-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.public-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.public-main {
  flex: 1;
  min-height: 0;
}
</style>
