<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  fetchUserDetail,
  fetchUsers,
  updateUserRole,
  updateUserStatus,
} from '@/api/admin'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import { useAuthStore } from '@/stores/auth'
import type { AdminUserVO, PageResult } from '@/types/api'
import {
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'

const auth = useAuthStore()

const keyword = ref('')
const role = ref<number | undefined>(undefined)
const status = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(10)

const detailVisible = ref(false)
const detailLoading = ref(false)
const active = ref<AdminUserVO | null>(null)

const roleVisible = ref(false)
const roleSubmitting = ref(false)
const roleTarget = ref<AdminUserVO | null>(null)
const newRole = ref(0)

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<AdminUserVO>
>(() =>
  fetchUsers({
    keyword: keyword.value.trim() || undefined,
    role: role.value === 0 || role.value === 1 ? role.value : undefined,
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

function isSelf(row: AdminUserVO): boolean {
  return auth.user?.id === row.id
}

async function openDetail(row: AdminUserVO): Promise<void> {
  active.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    active.value = await fetchUserDetail(row.id)
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

async function toggleStatus(row: AdminUserVO): Promise<void> {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定${action}用户「${row.username}」吗？${nextStatus === 0 ? '该用户所有 Token 将立即失效。' : ''}`,
      `${action}用户`,
      { type: 'warning', confirmButtonText: action, cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await updateUserStatus(row.id, nextStatus)
    ElMessage.success(`已${action}`)
    void run()
  } catch {
    // 错误已提示
  }
}

function openRole(row: AdminUserVO): void {
  roleTarget.value = row
  newRole.value = row.role
  roleVisible.value = true
}

async function submitRole(): Promise<void> {
  if (!roleTarget.value) return
  roleSubmitting.value = true
  try {
    await updateUserRole(roleTarget.value.id, newRole.value)
    ElMessage.success('角色已更新，该用户需要重新登录')
    roleVisible.value = false
    void run()
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    roleSubmitting.value = false
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
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">禁用用户或修改角色后，该用户已签发的 Token 会立即失效</p>
      </div>
      <el-button @click="run">刷新</el-button>
    </div>

    <div class="surface surface-pad">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="用户名 / 邮箱 / 手机号"
          clearable
          style="width: 240px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select v-model="role" placeholder="全部角色" clearable style="width: 130px" @change="reload">
          <el-option label="普通用户" :value="0" />
          <el-option label="管理员" :value="1" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
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
        <el-empty v-if="!data || data.records.length === 0" description="暂无用户" />
        <template v-else>
          <div class="surface table-surface">
            <el-table :data="data.records" style="width: 100%">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="username" label="用户名" min-width="140">
                <template #default="{ row }">
                  {{ row.username }}
                  <el-tag v-if="isSelf(row)" size="small" effect="plain" type="info">我</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="email" label="邮箱" min-width="180">
                <template #default="{ row }">{{ row.email || '—' }}</template>
              </el-table-column>
              <el-table-column prop="phone" label="手机号" width="130">
                <template #default="{ row }">{{ row.phone || '—' }}</template>
              </el-table-column>
              <el-table-column label="角色" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.role === 1 ? 'danger' : 'info'" effect="plain">
                    {{ row.role === 1 ? '管理员' : '普通用户' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
                    {{ row.status === 1 ? '正常' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="注册时间" width="170" />
              <el-table-column label="操作" width="210" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="isSelf(row)"
                    @click="toggleStatus(row)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                  <el-button link type="primary" size="small" :disabled="isSelf(row)" @click="openRole(row)">
                    改角色
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

    <el-drawer v-model="detailVisible" title="用户详情" size="420px">
      <el-skeleton v-if="detailLoading" :rows="6" animated />
      <el-descriptions v-else-if="active" :column="1" border>
        <el-descriptions-item label="ID">{{ active.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ active.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ active.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ active.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ active.role === 1 ? '管理员' : '普通用户' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ active.status === 1 ? '正常' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ active.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ active.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="roleVisible" title="修改用户角色" width="440px">
      <p class="role-tip">
        用户：{{ roleTarget?.username }}。修改角色后该用户所有 Token 会立即失效。
      </p>
      <el-radio-group v-model="newRole">
        <el-radio :value="0">普通用户</el-radio>
        <el-radio :value="1">管理员</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--cn-muted);
  line-height: 1.7;
}
</style>
