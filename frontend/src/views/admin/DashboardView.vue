<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { fetchDashboard } from '@/api/admin'
import DataState from '@/components/DataState.vue'
import { useAsync } from '@/composables/useAsync'
import type { DashboardVO } from '@/types/api'

const router = useRouter()

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<DashboardVO>(
  fetchDashboard,
)

const maxCategoryCount = computed(() => {
  const counts = data.value?.hotCategories.map((item) => item.count) ?? []
  return Math.max(1, ...counts)
})

const stats = computed(() => {
  if (!data.value) return []
  return [
    { label: '知识条目', value: data.value.totalKnowledge, hint: '已发布知识总数' },
    { label: '今日提问', value: data.value.todayQuestions, hint: '今日问答请求' },
    { label: '回答率', value: `${data.value.answerRate ?? 0}%`, hint: '有答案的提问占比' },
    { label: '注册用户', value: data.value.totalUsers, hint: '平台累计用户' },
    { label: '待审核贡献', value: data.value.pendingContributions, hint: '等待处理的贡献' },
  ]
})

onMounted(() => {
  void run()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">仪表盘</h1>
        <p class="page-subtitle">平台运行概况与待办事项</p>
      </div>
      <div class="toolbar">
        <el-button @click="router.push('/admin/knowledge/upload')">上传文档</el-button>
        <el-button type="primary" @click="router.push('/admin/contributions')">处理贡献</el-button>
      </div>
    </div>

    <DataState
      :loading="loading"
      :not-ready="notReady"
      :unreachable="unreachable"
      :error="errorMessage"
      :skeleton-rows="7"
      @retry="run"
    >
      <div class="stat-grid">
        <div v-for="item in stats" :key="item.label" class="stat-card surface">
          <span class="stat-label">{{ item.label }}</span>
          <strong class="stat-value">{{ item.value }}</strong>
          <span class="stat-hint">{{ item.hint }}</span>
        </div>
      </div>

      <div class="grid-2">
        <div class="surface surface-pad">
          <h2 class="section-title">热门分类</h2>
          <el-empty
            v-if="!data?.hotCategories?.length"
            description="暂无分类统计数据"
            :image-size="60"
          />
          <div v-else class="bar-list">
            <div v-for="item in data.hotCategories" :key="item.name" class="bar-row">
              <span class="bar-name">{{ item.name }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ width: `${Math.round((item.count / maxCategoryCount) * 100)}%` }"
                />
              </div>
              <span class="bar-value">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <div class="surface surface-pad">
          <h2 class="section-title">快捷入口</h2>
          <div class="quick-grid">
            <button type="button" class="quick-item" @click="router.push('/admin/knowledge')">
              知识管理
              <small>查看、编辑、删除知识</small>
            </button>
            <button type="button" class="quick-item" @click="router.push('/admin/feedback')">
              反馈处理
              <small>处理点赞踩与纠错</small>
            </button>
            <button type="button" class="quick-item" @click="router.push('/admin/tasks')">
              异步任务
              <small>向量化与批次回滚</small>
            </button>
            <button type="button" class="quick-item" @click="router.push('/admin/question-logs')">
              提问日志
              <small>查看问答记录与来源</small>
            </button>
          </div>
        </div>
      </div>
    </DataState>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 13px;
  color: var(--cn-muted);
}

.stat-value {
  font-size: 28px;
  line-height: 1.2;
}

.stat-hint {
  font-size: 11px;
  color: #9ca3af;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.section-title {
  margin: 0 0 18px;
  font-size: 16px;
}

.bar-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bar-row {
  display: grid;
  grid-template-columns: 80px 1fr 48px;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.bar-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  height: 8px;
  border-radius: 999px;
  background: #f1f5f9;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #2563eb);
}

.bar-value {
  text-align: right;
  color: var(--cn-muted);
  font-size: 12px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--cn-border);
  border-radius: 10px;
  background: #fff;
  color: var(--cn-text);
  font-size: 14px;
  cursor: pointer;
  text-align: left;
}

.quick-item:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.quick-item small {
  color: var(--cn-muted);
  font-size: 11px;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .stat-grid,
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
