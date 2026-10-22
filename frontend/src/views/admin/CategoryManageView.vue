<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createCategory,
  deleteCategory,
  fetchAdminCategories,
  updateCategory,
} from '@/api/admin'
import DataState from '@/components/DataState.vue'
import { useAsync } from '@/composables/useAsync'
import type { AdminCategoryVO } from '@/types/api'
import { BackendUnreachableError } from '@/utils/errors'

const status = ref<number | undefined>(undefined)
const keyword = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 32, message: '分类名称不能超过 32 字', trigger: 'blur' },
  ],
}

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  AdminCategoryVO[]
>(() =>
  fetchAdminCategories({
    status: status.value === 0 || status.value === 1 ? status.value : undefined,
    keyword: keyword.value.trim() || undefined,
  }),
)

function search(): void {
  void run()
}

function openCreate(): void {
  editingId.value = null
  form.name = ''
  form.sort = 0
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row: AdminCategoryVO): void {
  editingId.value = row.id
  form.name = row.name
  form.sort = row.sort
  form.status = row.status
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingId.value === null) {
      await createCategory({
        name: form.name.trim(),
        sort: form.sort,
        status: form.status,
      })
      ElMessage.success('分类已创建')
    } else {
      await updateCategory(editingId.value, {
        name: form.name.trim(),
        sort: form.sort,
        status: form.status,
      })
      ElMessage.success('分类已更新')
    }
    dialogVisible.value = false
    void run()
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    submitting.value = false
  }
}

async function removeCategory(row: AdminCategoryVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      row.knowledgeCount > 0
        ? `分类「${row.name}」下还有 ${row.knowledgeCount} 条知识，删除会被拒绝，建议改为停用。`
        : `确定删除分类「${row.name}」吗？`,
      '删除分类',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    void run()
  } catch {
    // 错误已提示（如 20012 分类正在使用）
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
        <h1 class="page-title">分类管理</h1>
        <p class="page-subtitle">分类被知识或贡献引用时不能删除，只能停用</p>
      </div>
      <el-button type="primary" @click="openCreate">新增分类</el-button>
    </div>

    <div class="surface surface-pad">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索分类名称"
          clearable
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
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
        <el-empty v-if="!data || data.length === 0" description="暂无分类" />
        <div v-else class="surface table-surface">
          <el-table :data="data" style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="分类名称" min-width="180" />
            <el-table-column prop="sort" label="排序" width="90" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="knowledgeCount" label="知识数" width="100" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column prop="updateTime" label="更新时间" width="170" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeCategory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </DataState>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId === null ? '新增分类' : '编辑分类'"
      width="460px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="排序值（越小越靠前）">
          <el-input-number v-model="form.sort" :min="-9999" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
