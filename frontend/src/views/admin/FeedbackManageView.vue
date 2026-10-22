<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchFeedbackList, processFeedback } from '@/api/feedback'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type { FeedbackVO, PageResult } from '@/types/api'
import { BackendUnreachableError } from '@/utils/errors'
import { feedbackTypeText, truncate } from '@/utils/format'

const feedbackType = ref<string | undefined>(undefined)
const status = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(10)

const processVisible = ref(false)
const processing = ref(false)
const active = ref<FeedbackVO | null>(null)
const processForm = reactive({ remark: '' })

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<FeedbackVO>
>(() =>
  fetchFeedbackList({
    feedbackType: feedbackType.value || undefined,
    status: status.value === 0 || status.value === 1 ? status.value : undefined,
    page: page.value,
    size: size.value,
  }),
)

function reload(): void {
  page.value = 1
  void run()
}

function changePage(nextPage: number, nextSize: number): void {
  page.value = nextPage
  size.value = nextSize
  void run()
}

function openProcess(row: FeedbackVO): void {
  active.value = row
  processForm.remark = ''
  processVisible.value = true
}

async function submitProcess(): Promise<void> {
  if (!active.value) return
  processing.value = true
  try {
    await processFeedback(active.value.id, {
      remark: processForm.remark.trim() || undefined,
    })
    ElMessage.success('反馈已处理')
    processVisible.value = false
    void run()
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    processing.value = false
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
        <h1 class="page-title">反馈处理</h1>
        <p class="page-subtitle">处理用户的点赞、点踩与纠错反馈</p>
      </div>
      <div class="toolbar">
        <el-select v-model="feedbackType" placeholder="全部类型" clearable style="width: 130px" @change="reload">
          <el-option label="点赞" value="like" />
          <el-option label="点踩" value="dislike" />
          <el-option label="纠错" value="report" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
          <el-option label="未处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
        <el-button @click="run">刷新</el-button>
      </div>
    </div>

    <DataState
      :loading="loading"
      :not-ready="notReady"
      :unreachable="unreachable"
      :error="errorMessage"
      @retry="run"
    >
      <el-empty v-if="!data || data.records.length === 0" description="暂无反馈记录" />
      <template v-else>
        <div class="surface table-surface">
          <el-table :data="data.records" style="width: 100%">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-box">
                  <p><strong>反馈内容：</strong>{{ row.content || '—' }}</p>
                  <p><strong>用户答案：</strong>{{ row.correctAnswer || '—' }}</p>
                  <p><strong>会话 ID：</strong><span class="mono">{{ row.sessionId || '—' }}</span></p>
                  <p><strong>关联知识：</strong>{{ row.knowledgeId ?? '—' }} · <strong>日志：</strong>{{ row.logId }}</p>
                  <p v-if="row.createdContributionId">
                    <strong>自动创建贡献：</strong>{{ row.createdContributionId }}
                  </p>
                  <p v-if="row.reviewRemark"><strong>处理备注：</strong>{{ row.reviewRemark }}</p>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userId" label="用户" width="90" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ feedbackTypeText(row.feedbackType) }}</template>
            </el-table-column>
            <el-table-column label="内容" min-width="220">
              <template #default="{ row }">
                <span class="text-muted">{{ truncate(row.content || row.correctAnswer, 40) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'" effect="plain">
                  {{ row.status === 1 ? '已处理' : '未处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" :disabled="row.status === 1" @click="openProcess(row)">
                  处理
                </el-button>
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

    <el-dialog v-model="processVisible" title="处理反馈" width="520px">
      <el-descriptions v-if="active" :column="1" border>
        <el-descriptions-item label="类型">{{ feedbackTypeText(active.feedbackType) }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容">{{ active.content || '—' }}</el-descriptions-item>
        <el-descriptions-item label="用户答案">{{ active.correctAnswer || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-position="top" class="mt-16">
        <el-form-item label="处理备注">
          <el-input
            v-model="processForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="记录处理方式，例如已修正知识条目"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="processing" @click="submitProcess">标记已处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.expand-box {
  padding: 8px 24px;
  font-size: 13px;
  line-height: 1.8;
  color: #475569;
}

.expand-box p {
  margin: 4px 0;
}
</style>
