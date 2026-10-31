<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchQuestionLogDetail, fetchQuestionLogs } from '@/api/admin'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type { PageResult, QuestionLogDetailVO, QuestionLogVO } from '@/types/api'
import {
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'
import {
  confidenceTagType,
  feedbackTypeText,
  formatConfidence,
  formatSource,
  truncate,
} from '@/utils/format'

const keyword = ref('')
const userId = ref<number | undefined>(undefined)
const sessionId = ref('')
const source = ref<string | undefined>(undefined)
const confidence = ref<string | undefined>(undefined)
const dateRange = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(10)

const detailVisible = ref(false)
const detailLoading = ref(false)
const active = ref<QuestionLogDetailVO | null>(null)

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<QuestionLogVO>
>(() =>
  fetchQuestionLogs({
    keyword: keyword.value.trim() || undefined,
    userId: userId.value ?? undefined,
    sessionId: sessionId.value.trim() || undefined,
    source: source.value || undefined,
    confidence: confidence.value || undefined,
    startTime: dateRange.value?.[0],
    endTime: dateRange.value?.[1],
    page: page.value,
    size: size.value,
  }),
)

function reload(): void {
  page.value = 1
  void run()
}

function resetFilters(): void {
  keyword.value = ''
  userId.value = undefined
  sessionId.value = ''
  source.value = undefined
  confidence.value = undefined
  dateRange.value = null
  reload()
}

function changePage(nextPage: number, nextSize: number): void {
  page.value = nextPage
  size.value = nextSize
  void run()
}

async function openDetail(row: QuestionLogVO): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  active.value = null
  try {
    active.value = await fetchQuestionLogDetail(row.id)
  } catch (error) {
    if (error instanceof BackendNotImplementedError) {
      ElMessage.warning(`后端接口尚未实现：${error.method} ${error.path}`)
    } else if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  void run()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">提问日志</h1>
        <p class="page-subtitle">查看用户提问、回答来源与命中知识，仅管理员可见</p>
      </div>
      <el-button @click="run">刷新</el-button>
    </div>

    <div class="surface surface-pad">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="问题关键词"
          clearable
          style="width: 200px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-input-number v-model="userId" :min="1" placeholder="用户 ID" :controls="false" style="width: 130px" />
        <el-input v-model="sessionId" placeholder="会话 UUID" clearable style="width: 220px" />
        <el-select v-model="source" placeholder="全部来源" clearable style="width: 150px" @change="reload">
          <el-option label="知识库" value="rag_knowledge_base" />
          <el-option label="AI 生成" value="ai_generated" />
        </el-select>
        <el-select v-model="confidence" placeholder="全部可信度" clearable style="width: 150px" @change="reload">
          <el-option label="高" value="high" />
          <el-option label="中" value="medium" />
          <el-option label="低" value="low" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
        />
        <el-button type="primary" @click="reload">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>

    <div class="mt-16">
      <DataState
        :loading="loading"
        :not-ready="notReady"
        :unreachable="unreachable"
        :error="errorMessage"
        @retry="run"
      >
        <el-empty v-if="!data || data.records.length === 0" description="暂无提问日志" />
        <template v-else>
          <div class="surface table-surface">
            <el-table :data="data.records" style="width: 100%">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column label="用户" width="110">
                <template #default="{ row }">
                  {{ row.username || '匿名' }}
                </template>
              </el-table-column>
              <el-table-column label="问题" min-width="260">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openDetail(row)">
                    {{ truncate(row.question, 46) }}
                  </el-button>
                </template>
              </el-table-column>
              <el-table-column label="可信度" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="confidenceTagType(row.confidence)" effect="plain">
                    {{ formatConfidence(row.confidence) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="来源" width="100">
                <template #default="{ row }">{{ formatSource(row.source) }}</template>
              </el-table-column>
              <el-table-column prop="matchCount" label="命中" width="80" />
              <el-table-column prop="createTime" label="提问时间" width="170" />
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <PaginationBar
            :total="data.total"
            :page="data.page"
            :size="data.size"
            @change="changePage"
          />
        </template>
      </DataState>
    </div>

    <el-drawer v-model="detailVisible" title="提问日志详情" size="600px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="active">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="日志 ID">{{ active.id }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ active.username || '匿名' }}</el-descriptions-item>
          <el-descriptions-item label="可信度">{{ formatConfidence(active.confidence) }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ formatSource(active.source) }}</el-descriptions-item>
          <el-descriptions-item label="命中知识数">{{ active.matchCount }}</el-descriptions-item>
          <el-descriptions-item label="提问时间">{{ active.createTime }}</el-descriptions-item>
          <el-descriptions-item label="会话 ID" :span="2">
            <span class="mono">{{ active.sessionId }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="section-label">问题</h3>
        <div class="block">{{ active.question }}</div>

        <template v-if="active.rewrittenQuery">
          <h3 class="section-label">改写后的检索词</h3>
          <div class="block">{{ active.rewrittenQuery }}</div>
        </template>

        <h3 class="section-label">回答</h3>
        <pre class="block answer">{{ active.answerContent || '—' }}</pre>

        <h3 class="section-label">引用知识</h3>
        <div v-if="active.citations?.length" class="citations">
          <el-tag v-for="item in active.citations" :key="item.knowledgeId" effect="plain">
            {{ item.title }}（#{{ item.knowledgeId }}）
          </el-tag>
        </div>
        <p v-else class="text-muted text-small">无引用</p>

        <h3 class="section-label">关联反馈</h3>
        <div v-if="active.feedbacks?.length" class="surface table-surface">
          <el-table :data="active.feedbacks" size="small">
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ feedbackTypeText(row.feedbackType) }}</template>
            </el-table-column>
            <el-table-column label="内容" min-width="160">
              <template #default="{ row }">{{ row.content || row.correctAnswer || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">{{ row.status === 1 ? '已处理' : '未处理' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
        </div>
        <p v-else class="text-muted text-small">无关联反馈</p>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.section-label {
  margin: 22px 0 10px;
  font-size: 14px;
}

.block {
  margin: 0;
  padding: 14px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid var(--cn-border);
  font-family: inherit;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.block.answer {
  max-height: 360px;
  overflow-y: auto;
}

.citations {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
