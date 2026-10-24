<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification, type FormInstance, type FormRules } from 'element-plus'

import {
  createKnowledge,
  deleteKnowledge,
  fetchAdminCategories,
  fetchAdminKnowledge,
  triggerVectorize,
  updateKnowledge,
} from '@/api/admin'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type {
  AdminCategoryVO,
  AdminKnowledgeVO,
  KnowledgeUpdateDTO,
  PageResult,
} from '@/types/api'
import { ApiError, BackendUnreachableError } from '@/utils/errors'
import { truncate, vectorStatusTagType, vectorStatusText } from '@/utils/format'

const router = useRouter()

const keyword = ref('')
const categoryId = ref<number | undefined>(undefined)
const status = ref<number | undefined>(undefined)
const importBatchId = ref('')
const page = ref(1)
const size = ref(10)

const categories = ref<AdminCategoryVO[]>([])

const editingId = ref<number | null>(null)
const originalCategoryId = ref<number | null>(null)
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const vectorizing = ref(false)

const form = reactive({
  categoryId: null as number | null,
  title: '',
  question: '',
  answer: '',
  keywords: '',
})

const formRules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ max: 200, message: '标题不能超过 200 字', trigger: 'blur' }],
  question: [{ max: 500, message: '标准问题不能超过 500 字', trigger: 'blur' }],
  answer: [{ max: 5000, message: '答案不能超过 5000 字', trigger: 'blur' }],
  keywords: [{ max: 500, message: '关键词不能超过 500 字', trigger: 'blur' }],
}

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<AdminKnowledgeVO>
>(() =>
  fetchAdminKnowledge({
    keyword: keyword.value.trim() || undefined,
    categoryId: categoryId.value || undefined,
    status: status.value === 0 || status.value === 1 ? status.value : undefined,
    importBatchId: importBatchId.value.trim() || undefined,
    page: page.value,
    size: size.value,
  }),
)

function loadCategories(): void {
  void fetchAdminCategories()
    .then((list) => {
      categories.value = list
    })
    .catch(() => {
      categories.value = []
    })
}

function categoryLabel(item: AdminCategoryVO): string {
  return item.status === 0 ? `${item.name}（已停用）` : item.name
}

function search(): void {
  page.value = 1
  void run()
}

function changePage(nextPage: number, nextSize: number): void {
  page.value = nextPage
  size.value = nextSize
  void run()
}

function resetForm(): void {
  form.categoryId = null
  form.title = ''
  form.question = ''
  form.answer = ''
  form.keywords = ''
}

function openCreate(): void {
  editingId.value = null
  originalCategoryId.value = null
  resetForm()
  formVisible.value = true
}

function openEdit(row: AdminKnowledgeVO): void {
  editingId.value = row.id
  originalCategoryId.value = row.categoryId
  form.categoryId = row.categoryId
  form.title = row.title ?? ''
  form.question = row.question ?? ''
  form.answer = row.answer ?? ''
  form.keywords = row.keywords ?? ''
  formVisible.value = true
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || form.categoryId === null) return

  const hasContent = form.title.trim() || form.question.trim() || form.answer.trim()
  if (!hasContent) {
    ElMessage.warning('标题、标准问题、答案至少填写一项')
    return
  }
  if (form.question.trim() && !form.answer.trim()) {
    ElMessage.warning('填写标准问题时必须同时填写答案')
    return
  }

  const selectedCategoryId = form.categoryId
  submitting.value = true
  try {
    const content = {
      title: form.title.trim() || undefined,
      question: form.question.trim() || undefined,
      answer: form.answer.trim() || undefined,
      keywords: form.keywords.trim() || undefined,
    }
    if (editingId.value === null) {
      await createKnowledge({ categoryId: selectedCategoryId, ...content })
      ElMessage.success('知识已录入，向量状态为待同步')
    } else {
      const payload: KnowledgeUpdateDTO = { ...content }
      if (selectedCategoryId !== originalCategoryId.value) {
        payload.categoryId = selectedCategoryId
      }
      await updateKnowledge(editingId.value, payload)
      ElMessage.success('知识已更新，需要重新同步向量')
    }
    formVisible.value = false
    void run()
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    submitting.value = false
  }
}

async function removeKnowledge(row: AdminKnowledgeVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.title || row.question || row.id}」吗？删除后需重新同步向量。`,
      '删除知识',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteKnowledge(row.id)
    ElMessage.success('已删除')
    void run()
  } catch {
    // 错误已提示
  }
}

async function vectorize(): Promise<void> {
  vectorizing.value = true
  try {
    const result = await triggerVectorize()
    ElNotification({
      title: '向量化任务已创建',
      message: `taskId：${result.taskId}，待处理 ${result.pendingCount} 条`,
      type: 'success',
      duration: 6000,
      onClick: () => router.push('/admin/tasks'),
    })
  } catch (error) {
    if (error instanceof ApiError) {
      ElMessage.error(error.message)
    } else if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    vectorizing.value = false
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
        <h1 class="page-title">知识管理</h1>
        <p class="page-subtitle">维护知识条目，编辑后需要重新同步向量</p>
      </div>
      <div class="toolbar">
        <el-button :loading="vectorizing" @click="vectorize">同步向量</el-button>
        <el-button @click="router.push('/admin/knowledge/upload')">上传文档</el-button>
        <el-button type="primary" @click="openCreate">手动录入</el-button>
      </div>
    </div>

    <div class="surface surface-pad">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索标题/问题/关键词"
          clearable
          style="width: 240px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select
          v-model="categoryId"
          placeholder="全部分类"
          clearable
          style="width: 160px"
          @change="search"
        >
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="categoryLabel(item)"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="正常" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-input
          v-model="importBatchId"
          placeholder="导入批次 ID"
          clearable
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
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
        <el-empty v-if="!data || data.records.length === 0" description="暂无知识条目" />
        <template v-else>
          <div class="surface table-surface">
            <el-table :data="data.records" style="width: 100%">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column label="标题 / 标准问题" min-width="240">
                <template #default="{ row }">
                  <div class="title-cell">{{ row.title || row.question || '未命名' }}</div>
                  <div class="sub-cell">{{ truncate(row.question && row.question !== row.title ? row.question : row.answer, 42) }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="categoryName" label="分类" width="110" />
              <el-table-column label="来源" width="100">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain" type="info">
                    {{ row.sourceType === 'upload' ? '文档导入' : '手动录入' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="向量状态" width="110">
                <template #default="{ row }">
                  <el-tooltip
                    :content="row.vectorError || row.vectorSyncedTime || '尚未同步向量'"
                    placement="top"
                  >
                    <el-tag size="small" :type="vectorStatusTagType(row.vectorStatus)" effect="plain">
                      {{ vectorStatusText(row.vectorStatus) }}
                    </el-tag>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'" effect="plain">
                    {{ row.status === 1 ? '正常' : '下架' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="viewCount" label="浏览" width="80" />
              <el-table-column prop="createTime" label="创建时间" width="170" />
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="removeKnowledge(row)">删除</el-button>
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

    <el-dialog
      v-model="formVisible"
      :title="editingId === null ? '手动录入知识' : '编辑知识'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="categoryLabel(item)"
              :value="item.id"
              :disabled="item.status === 0 && item.id !== originalCategoryId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题（可选）" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="标准问题（可选，填写时答案必填）" prop="question">
          <el-input v-model="form.question" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="标准答案" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="6" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="关键词（英文逗号分隔）" prop="keywords">
          <el-input v-model="form.keywords" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.title-cell {
  font-size: 13px;
  font-weight: 500;
}

.sub-cell {
  margin-top: 2px;
  font-size: 12px;
  color: var(--cn-muted);
}
</style>
