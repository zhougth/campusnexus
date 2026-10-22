<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import {
  fetchAdminContributionDetail,
  fetchAdminContributions,
  reviewContribution,
} from '@/api/admin'
import { fetchCategories } from '@/api/common'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type { AdminContributionVO, CategoryVO, PageResult } from '@/types/api'
import {
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'
import {
  contributionStatusTagType,
  contributionStatusText,
  contributionTypeText,
  truncate,
} from '@/utils/format'

const status = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(10)
const categories = ref<CategoryVO[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const active = ref<AdminContributionVO | null>(null)

const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref<FormInstance>()
const reviewTarget = ref<AdminContributionVO | null>(null)
const reviewForm = reactive({
  status: 1 as 1 | 2,
  remark: '',
  categoryId: null as number | null,
})

const reviewRules: FormRules = {
  status: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  remark: [{ max: 500, message: '审核备注不能超过 500 字', trigger: 'blur' }],
}

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<AdminContributionVO>
>(() =>
  fetchAdminContributions({
    status: status.value === 0 || status.value === 1 || status.value === 2 ? status.value : undefined,
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

async function openDetail(row: AdminContributionVO): Promise<void> {
  active.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    active.value = await fetchAdminContributionDetail(row.id)
  } catch (error) {
    if (error instanceof BackendNotImplementedError) {
      ElMessage.warning(`后端接口尚未实现：${error.method} ${error.path}`)
    } else if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    detailLoading.value = false
  }
}

function openReview(row: AdminContributionVO): void {
  reviewTarget.value = row
  reviewForm.status = 1
  reviewForm.remark = ''
  reviewForm.categoryId = row.categoryId
  reviewVisible.value = true
}

async function submitReview(): Promise<void> {
  if (!reviewFormRef.value || !reviewTarget.value) return
  const valid = await reviewFormRef.value.validate().catch(() => false)
  if (!valid) return

  reviewSubmitting.value = true
  try {
    await reviewContribution(reviewTarget.value.id, {
      status: reviewForm.status,
      remark: reviewForm.remark.trim() || undefined,
      categoryId: reviewForm.categoryId ?? undefined,
    })
    ElMessage.success(reviewForm.status === 1 ? '已通过审核' : '已驳回')
    reviewVisible.value = false
    void run()
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    reviewSubmitting.value = false
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
        <h1 class="page-title">贡献审核</h1>
        <p class="page-subtitle">审核用户提交的知识贡献，通过后写入知识库并等待同步向量</p>
      </div>
      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="reload">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
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
      <el-empty v-if="!data || data.records.length === 0" description="暂无贡献记录" />
      <template v-else>
        <div class="surface table-surface">
          <el-table :data="data.records" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="username" label="提交人" width="120" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ contributionTypeText(row.type) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="contributionStatusTagType(row.status)" effect="plain">
                  {{ contributionStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核备注" min-width="160">
              <template #default="{ row }">
                <span class="text-muted">{{ truncate(row.remark, 28) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                <el-button
                  link
                  type="primary"
                  size="small"
                  :disabled="row.status !== 0"
                  @click="openReview(row)"
                >
                  审核
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

    <el-drawer v-model="detailVisible" title="贡献详情" size="560px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="active">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ active.id }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ active.title }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ active.username }}（ID {{ active.userId }}）</el-descriptions-item>
          <el-descriptions-item label="类型">{{ contributionTypeText(active.type) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="contributionStatusTagType(active.status)" effect="plain">
              {{ contributionStatusText(active.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户补充说明">{{ active.submitRemark || '—' }}</el-descriptions-item>
          <el-descriptions-item label="审核备注">{{ active.remark || '—' }}</el-descriptions-item>
          <el-descriptions-item label="写入知识 ID">{{ active.knowledgeId ?? '—' }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="content-title">内容</h3>
        <pre class="content-block">{{ active.content }}</pre>
      </template>
    </el-drawer>

    <el-dialog v-model="reviewVisible" title="审核贡献" width="560px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-position="top">
        <el-form-item label="审核结果" prop="status">
          <el-radio-group v-model="reviewForm.status">
            <el-radio-button :value="1">通过并写入知识库</el-radio-button>
            <el-radio-button :value="2">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="reviewForm.status === 1" label="写入分类">
          <el-select v-model="reviewForm.categoryId" clearable placeholder="保持原分类" style="width: 100%">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核备注" prop="remark">
          <el-input
            v-model="reviewForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="驳回时建议说明原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.content-title {
  margin: 20px 0 10px;
  font-size: 14px;
}

.content-block {
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
</style>
