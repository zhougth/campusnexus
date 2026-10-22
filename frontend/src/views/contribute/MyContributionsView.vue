<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchMyContributions } from '@/api/contribute'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type { ContributionVO, PageResult } from '@/types/api'
import {
  contributionStatusTagType,
  contributionStatusText,
  contributionTypeText,
  truncate,
} from '@/utils/format'

const router = useRouter()

const status = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(10)
const detailVisible = ref(false)
const active = ref<ContributionVO | null>(null)

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<ContributionVO>
>(() =>
  fetchMyContributions({
    page: page.value,
    size: size.value,
    status:
      status.value === 0 || status.value === 1 || status.value === 2
        ? status.value
        : undefined,
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

function openDetail(item: ContributionVO): void {
  active.value = item
  detailVisible.value = true
}

onMounted(() => {
  void run()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">我的贡献</h1>
        <p class="page-subtitle">查看你提交的知识贡献与审核结果</p>
      </div>
      <div class="toolbar">
        <el-select
          v-model="status"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="reload"
        >
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button type="primary" @click="router.push('/contribute')">提交新贡献</el-button>
      </div>
    </div>

    <DataState
      :loading="loading"
      :not-ready="notReady"
      :unreachable="unreachable"
      :error="errorMessage"
      @retry="run"
    >
      <el-empty v-if="!data || data.records.length === 0" description="还没有提交过贡献" />
      <template v-else>
        <div class="surface table-surface">
          <el-table :data="data.records" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="200">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">{{ row.title }}</el-button>
              </template>
            </el-table-column>
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
            <el-table-column label="备注" min-width="180">
              <template #default="{ row }">
                <span class="text-muted">{{ truncate(row.remark, 30) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="180" />
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

    <el-drawer v-model="detailVisible" title="贡献详情" size="520px">
      <template v-if="active">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ active.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ contributionTypeText(active.type) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="contributionStatusTagType(active.status)" effect="plain">
              {{ contributionStatusText(active.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ active.createTime }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ active.remark || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="content-title">内容</h3>
        <pre class="content-block">{{ active.content }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.content-title {
  margin: 22px 0 10px;
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
