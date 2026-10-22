<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchTaskDetail, fetchTasks } from '@/api/admin'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type { AsyncTaskVO, PageResult, TaskStatus, TaskType } from '@/types/api'
import {
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'
import { taskStatusTagType, taskStatusText } from '@/utils/format'

const taskType = ref<TaskType | undefined>(undefined)
const status = ref<TaskStatus | undefined>(undefined)
const page = ref(1)
const size = ref(10)

const detailVisible = ref(false)
const detailLoading = ref(false)
const active = ref<AsyncTaskVO | null>(null)

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<AsyncTaskVO>
>(() =>
  fetchTasks({
    taskType: taskType.value || undefined,
    status: status.value || undefined,
    page: page.value,
    size: size.value,
  }),
)

let timer: number | null = null

function reload(): void {
  page.value = 1
  void run()
}

function changePage(nextPage: number, nextSize: number): void {
  page.value = nextPage
  size.value = nextSize
  void run()
}

function hasActiveTask(): boolean {
  return Boolean(
    data.value?.records.some((task) => task.status === 'PENDING' || task.status === 'RUNNING'),
  )
}

async function openDetail(row: AsyncTaskVO): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  active.value = null
  try {
    active.value = await fetchTaskDetail(row.taskId)
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
  timer = window.setInterval(() => {
    if (hasActiveTask()) {
      void run()
    }
  }, 3000)
})

onUnmounted(() => {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">异步任务</h1>
        <p class="page-subtitle">向量化与批次回滚任务共用同一套任务记录，执行中会自动刷新</p>
      </div>
      <div class="toolbar">
        <el-select v-model="taskType" placeholder="全部类型" clearable style="width: 150px" @change="reload">
          <el-option label="向量化" value="VECTORIZE" />
          <el-option label="批次回滚" value="BATCH_ROLLBACK" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
          <el-option label="等待中" value="PENDING" />
          <el-option label="执行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="部分成功" value="PARTIAL" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button @click="run">刷新</el-button>
      </div>
    </div>

    <DataState
      :loading="loading && !data"
      :not-ready="notReady"
      :unreachable="unreachable"
      :error="errorMessage"
      @retry="run"
    >
      <el-empty v-if="!data || data.records.length === 0" description="暂无异步任务" />
      <template v-else>
        <div class="surface table-surface">
          <el-table :data="data.records" style="width: 100%">
            <el-table-column label="任务 ID" min-width="220">
              <template #default="{ row }">
                <el-button link type="primary" class="mono" @click="openDetail(row)">
                  {{ row.taskId }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                {{ row.taskType === 'VECTORIZE' ? '向量化' : '批次回滚' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="taskStatusTagType(row.status)" effect="plain">
                  {{ taskStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" min-width="200">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.progress"
                  :status="row.status === 'FAILED' ? 'exception' : row.status === 'SUCCESS' ? 'success' : undefined"
                />
              </template>
            </el-table-column>
            <el-table-column label="成功 / 失败" width="120">
              <template #default="{ row }">
                <span class="success-text">{{ row.success }}</span>
                <span class="text-muted"> / </span>
                <span class="failed-text">{{ row.failed }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
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

    <el-drawer v-model="detailVisible" title="任务详情" size="520px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="active">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务 ID"><span class="mono">{{ active.taskId }}</span></el-descriptions-item>
          <el-descriptions-item label="类型">
            {{ active.taskType === 'VECTORIZE' ? '向量化' : '批次回滚' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="taskStatusTagType(active.status)" effect="plain">
              {{ taskStatusText(active.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="进度">
            <el-progress :percentage="active.progress" style="width: 100%" />
          </el-descriptions-item>
          <el-descriptions-item label="总数">{{ active.total }}</el-descriptions-item>
          <el-descriptions-item label="已处理">{{ active.processed }}</el-descriptions-item>
          <el-descriptions-item label="成功 / 失败">{{ active.success }} / {{ active.failed }}</el-descriptions-item>
          <el-descriptions-item label="任务参数">
            <span class="mono">{{ JSON.stringify(active.payload) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="错误信息">{{ active.errorMessage || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ active.createdBy }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ active.startedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ active.finishedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ active.createTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.success-text {
  color: #16a34a;
}

.failed-text {
  color: #dc2626;
}
</style>
