<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchKnowledgeDetail } from '@/api/common'
import DataState from '@/components/DataState.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import { useAsync } from '@/composables/useAsync'
import type { KnowledgeVO } from '@/types/api'

const route = useRoute()
const router = useRouter()

const knowledgeId = computed(() => Number(route.params.id))

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<KnowledgeVO>(() =>
  fetchKnowledgeDetail(knowledgeId.value),
)

const keywordList = computed(() => {
  const raw = data.value?.keywords
  if (!raw) return []
  return raw
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
})

onMounted(() => {
  void run()
})

watch(knowledgeId, () => {
  void run()
})
</script>

<template>
  <div class="page-container">
    <div class="back-row">
      <el-button link @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <el-button link type="primary" @click="router.push('/knowledge')">知识库首页</el-button>
    </div>

    <DataState
      :loading="loading"
      :not-ready="notReady"
      :unreachable="unreachable"
      :error="errorMessage"
      :skeleton-rows="8"
      @retry="run"
    >
      <article v-if="data" class="detail-card surface">
        <div class="detail-head">
          <div class="tags">
            <el-tag size="small" effect="plain">{{ data.categoryName }}</el-tag>
            <el-tag size="small" type="info" effect="plain">
              {{ data.sourceType === 'upload' ? '文档导入' : '手动录入' }}
            </el-tag>
            <el-tag v-if="data.status === 0" size="small" type="warning" effect="plain">已下架</el-tag>
          </div>
          <span class="view-count">
            <el-icon><View /></el-icon>
            {{ data.viewCount }} 次浏览
          </span>
        </div>

        <h1>{{ data.title || data.question || '知识详情' }}</h1>

        <div v-if="data.question && data.question !== data.title" class="question">
          <span class="label">标准问题</span>
          <p>{{ data.question }}</p>
        </div>

        <div class="answer">
          <span class="label">标准答案</span>
          <MarkdownView v-if="data.answer" :content="data.answer" />
          <p v-else class="text-muted">暂无答案内容</p>
        </div>

        <div v-if="keywordList.length" class="keywords">
          <span class="label">关键词</span>
          <el-tag v-for="keyword in keywordList" :key="keyword" size="small" effect="plain" type="info">
            {{ keyword }}
          </el-tag>
        </div>

        <div v-if="data.fileUrl" class="attachment">
          <span class="label">原始文档</span>
          <a :href="data.fileUrl" target="_blank" rel="noopener noreferrer">
            {{ data.originalFilename || '查看文件' }}
          </a>
        </div>

        <div class="detail-foot">
          <span>创建于 {{ data.createTime }}</span>
          <el-button link type="primary" @click="router.push('/contribute')">提交纠错/贡献</el-button>
        </div>
      </article>
    </DataState>
  </div>
</template>

<style scoped>
.back-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.detail-card {
  padding: 28px 32px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.view-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.detail-card h1 {
  margin: 16px 0 20px;
  font-size: 24px;
  line-height: 1.45;
}

.question {
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f8fafc;
}

.label {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.5px;
}

.question p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
}

.answer {
  margin-bottom: 20px;
}

.keywords,
.attachment {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.keywords .label,
.attachment .label {
  margin-bottom: 0;
}

.attachment a {
  color: var(--cn-primary);
  font-size: 13px;
}

.detail-foot {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}

@media (max-width: 720px) {
  .detail-card {
    padding: 20px;
  }
}
</style>
