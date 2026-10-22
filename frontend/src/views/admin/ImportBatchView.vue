<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'

import {
  fetchImportBatchDetail,
  fetchImportBatches,
  rollbackImportBatch,
} from '@/api/admin'
import { fetchCategories } from '@/api/common'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type {
  CategoryVO,
  ImportBatchDetailVO,
  ImportBatchVO,
  PageResult,
  VectorizeResponse,
} from '@/types/api'
import {
  ApiError,
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'
import { batchStatusTagType, batchStatusText } from '@/utils/format'

const router = useRouter()

const categoryId = ref<number | undefined>(undefined)
const status = ref<string | undefined>(undefined)
const dateRange = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(10)
const categories = ref<CategoryVO[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const active = ref<ImportBatchDetailVO | null>(null)

const rollbackVisible = ref(false)
const rollbackSubmitting = ref(false)
const rollbackTarget = ref<ImportBatchVO | null>(null)
const rollbackForm = reactive({
  force: false,
  deleteFile: false,
})

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<ImportBatchVO>
>(() =>
  fetchImportBatches({
    categoryId: categoryId.value || undefined,
    status: status.value || undefined,
    startTime: dateRange.value?.[0],
    endTime: dateRange.value?.[1],
    page: page.value,
    size: size.value,
  }),
)

function loadCategories(): void {
  void fetchCategories()
    .then((list) => {
      categories.value = list
    })
    .catch(() => {
      categories.value = []
    })
}

function reload(): void {
  page.value = 1
  void run()
}

function changePage(nextPage: number, nextSize: number): void {
  page.value = nextPage
  size.value = nextSize
  void run()
}

async function openDetail(row: ImportBatchVO): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  active.value = null
  try {
    active.value = await fetchImportBatchDetail(row.batchId)
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

function openRollback(row: ImportBatchVO): void {
  rollbackTarget.value = row
  rollbackForm.force = false
  rollbackForm.deleteFile = false
  rollbackVisible.value = true
}

async function submitRollback(): Promise<void> {
  if (!rollbackTarget.value) return
  rollbackSubmitting.value = true
  try {
    const result: VectorizeResponse = await rollbackImportBatch(rollbackTarget.value.batchId, {
      force: rollbackForm.force,
      deleteFile: rollbackForm.deleteFile,
    })
    rollbackVisible.value = false
    ElNotification({
      title: '批次回滚任务已创建',
      message: `taskId：${result.taskId}，待处理 ${result.pendingCount} 条`,
      type: 'success',
      duration: 6000,
      onClick: () => router.push('/admin/tasks'),
    })
    void run()
  } catch (error) {
    if (error instanceof BackendNotImplementedError) {
      ElMessage.warning(`后端接口尚未实现：${error.method} ${error.path}`)
    } else if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    } else if (error instanceof ApiError) {
      ElMessage.error(error.message)
    }
  } finally {
    rollbackSubmitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  void run()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">导入批次</h1>
        <p class="page-subtitle">追溯文档导入结果，必要时回滚整批知识</p>
      </div>
      <el-button @click="run">刷新</el-button>
    </div>

    <div class="surface surface-pad">
      <div class="toolbar">
        <el-select v-model="categoryId" placeholder="全部分类" clearable style="width: 160px" @change="reload">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已回滚" value="ROLLED_BACK" />
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
        <el-empty v-if="!data || data.records.length === 0" description="暂无导入批次" />
        <template v-else>
          <div class="surface table-surface">
            <el-table :data="data.records" style="width: 100%">
              <el-table-column label="批次 ID" min-width="220">
                <template #default="{ row }">
                  <span class="mono">{{ row.batchId }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="originalFilename" label="文件名" min-width="160" show-overflow-tooltip />
              <el-table-column prop="categoryName" label="分类" width="110" />
              <el-table-column prop="knowledgeCount" label="知识数" width="90" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="batchStatusTagType(row.status)" effect="plain">
                    {{ batchStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="导入时间" width="170" />
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    :disabled="row.status === 'ROLLED_BACK'"
                    @click="openRollback(row)"
                  >
                    回滚
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
    </div>

    <el-drawer v-model="detailVisible" title="批次详情" size="520px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="active">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="批次 ID"><span class="mono">{{ active.batchId }}</span></el-descriptions-item>
          <el-descriptions-item label="文件名">{{ active.originalFilename }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ active.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="batchStatusTagType(active.status)" effect="plain">
              {{ batchStatusText(active.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="知识数">{{ active.knowledgeCount }}</el-descriptions-item>
          <el-descriptions-item label="文档字符数">{{ active.totalChars }}</el-descriptions-item>
          <el-descriptions-item label="已同步向量">{{ active.vectorSyncedCount }}</el-descriptions-item>
          <el-descriptions-item label="待同步向量">{{ active.vectorPendingCount }}</el-descriptions-item>
          <el-descriptions-item label="同步失败">{{ active.vectorFailedCount }}</el-descriptions-item>
          <el-descriptions-item label="补充关键词">{{ active.keywords || '—' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息">{{ active.errorMessage || '—' }}</el-descriptions-item>
          <el-descriptions-item label="上传人">{{ active.createdBy }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ active.createTime }}</el-descriptions-item>
          <el-descriptions-item label="回滚时间">{{ active.rolledBackTime || '—' }}</el-descriptions-item>
        </el-descriptions>
        <p class="text-muted text-small mt-16">
          文件：
          <a :href="active.fileUrl" target="_blank" rel="noopener noreferrer">{{ active.fileUrl }}</a>
        </p>
      </template>
    </el-drawer>

    <el-dialog v-model="rollbackVisible" title="批次回滚" width="520px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="回滚会逻辑删除该批次生成的知识并清理向量"
        description="如果批次内有上传后被修改过的知识，需要勾选「强制回滚」。回滚任务在后台异步执行。"
      />
      <el-form label-position="top" class="mt-16">
        <el-form-item>
          <el-checkbox v-model="rollbackForm.force">强制回滚（忽略上传后修改检查）</el-checkbox>
          <el-checkbox v-model="rollbackForm.deleteFile">同时删除原始文件</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackVisible = false">取消</el-button>
        <el-button type="danger" :loading="rollbackSubmitting" @click="submitRollback">确认回滚</el-button>
      </template>
    </el-dialog>
  </div>
</template>
