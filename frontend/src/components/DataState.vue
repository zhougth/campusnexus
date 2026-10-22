<script setup lang="ts">
import type { NotReadyInfo } from '@/composables/useAsync'

defineProps<{
  loading?: boolean
  notReady?: NotReadyInfo | null
  unreachable?: boolean
  error?: string
  skeletonRows?: number
}>()

defineEmits<{
  retry: []
}>()
</script>

<template>
  <div v-if="notReady" class="state-block">
    <el-result icon="warning" title="后端接口尚未实现">
      <template #sub-title>
        <span class="code-line">{{ notReady.method }} {{ notReady.path }}</span>
      </template>
      <template #extra>
        <p class="text-muted state-tip">
          该页面已按接口文档接入，后端实现后无需改前端，刷新即可直接使用。
        </p>
        <el-button @click="$emit('retry')">重新检测</el-button>
      </template>
    </el-result>
  </div>

  <div v-else-if="unreachable" class="state-block">
    <el-result icon="error" title="无法连接后端服务">
      <template #sub-title>
        <span class="text-muted">请确认 Spring Boot 已启动（默认 http://localhost:8080）</span>
      </template>
      <template #extra>
        <el-button type="primary" @click="$emit('retry')">重试</el-button>
      </template>
    </el-result>
  </div>

  <div v-else-if="loading" class="state-block state-skeleton">
    <el-skeleton :rows="skeletonRows ?? 6" animated />
  </div>

  <div v-else-if="error" class="state-block">
    <el-result icon="error" title="加载失败" :sub-title="error">
      <template #extra>
        <el-button @click="$emit('retry')">重试</el-button>
      </template>
    </el-result>
  </div>

  <slot v-else />
</template>

<style scoped>
.state-block {
  padding: 12px 0;
}

.state-skeleton {
  padding: 24px;
  background: #fff;
  border: 1px solid var(--cn-border);
  border-radius: var(--cn-radius);
}

.state-tip {
  margin: 0 0 16px;
  font-size: 13px;
}
</style>
