<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  fetchCategories,
  searchKnowledge,
  searchKnowledgeByCategory,
} from '@/api/common'
import DataState from '@/components/DataState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAsync } from '@/composables/useAsync'
import type { CategoryVO, KnowledgeVO, PageResult } from '@/types/api'
import { truncate } from '@/utils/format'

const router = useRouter()

const keyword = ref('')
const activeCategory = ref<number | null>(null)
const page = ref(1)
const size = ref(10)
const categories = ref<CategoryVO[]>([])

const { data, loading, notReady, unreachable, errorMessage, run } = useAsync<
  PageResult<KnowledgeVO>
>(() => {
  const text = keyword.value.trim()
  if (!text && activeCategory.value) {
    return searchKnowledgeByCategory({
      categoryId: activeCategory.value,
      page: page.value,
      size: size.value,
    })
  }
  return searchKnowledge({ keyword: text, page: page.value, size: size.value })
})

function loadCategories(): void {
  void fetchCategories()
    .then((list) => {
      categories.value = list
    })
    .catch(() => {
      categories.value = []
    })
}

function search(): void {
  page.value = 1
  void run()
}

function selectCategory(id: number | null): void {
  activeCategory.value = id
  page.value = 1
  void run()
}

function changePage(nextPage: number, nextSize: number): void {
  page.value = nextPage
  size.value = nextSize
  void run()
}

function openDetail(id: number): void {
  void router.push(`/knowledge/${id}`)
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
        <h1 class="page-title">校园知识库</h1>
        <p class="page-subtitle">搜索校园生活、教务、后勤等常见问题的标准答案</p>
      </div>
    </div>

    <div class="search-panel surface surface-pad">
      <div class="search-row">
        <el-input
          v-model="keyword"
          size="large"
          clearable
          placeholder="搜索关键词，例如：食堂、图书馆、校车"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" @click="search">搜索</el-button>
      </div>

      <div v-if="categories.length" class="category-row">
        <button
          type="button"
          class="category-pill"
          :class="{ active: activeCategory === null }"
          @click="selectCategory(null)"
        >
          全部
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="category-pill"
          :class="{ active: activeCategory === category.id }"
          @click="selectCategory(category.id)"
        >
          {{ category.name }}
          <span class="pill-count">{{ category.knowledgeCount }}</span>
        </button>
      </div>
    </div>

    <div class="mt-16">
      <DataState
        :loading="loading"
        :not-ready="notReady"
        :unreachable="unreachable"
        :error="errorMessage"
        :skeleton-rows="8"
        @retry="run"
      >
        <el-empty
          v-if="!data || data.records.length === 0"
          description="没有找到相关知识，换个关键词试试"
        />
        <template v-else>
          <div class="knowledge-grid">
            <article
              v-for="item in data.records"
              :key="item.id"
              class="knowledge-card surface"
              @click="openDetail(item.id)"
            >
              <div class="card-top">
                <el-tag size="small" effect="plain">{{ item.categoryName }}</el-tag>
                <span class="view-count">
                  <el-icon><View /></el-icon>
                  {{ item.viewCount }}
                </span>
              </div>
              <h2>{{ item.title || item.question || '未命名知识' }}</h2>
              <p>{{ truncate(item.question || item.answer, 96) }}</p>
              <div class="card-foot">
                <span>{{ item.createTime }}</span>
                <el-button link type="primary" size="small">查看详情</el-button>
              </div>
            </article>
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
  </div>
</template>

<style scoped>
.search-panel {
  padding: 18px 20px;
}

.search-row {
  display: flex;
  gap: 12px;
}

.search-row .el-input {
  flex: 1;
}

.category-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.category-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--cn-border);
  background: #fff;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;
}

.category-pill:hover {
  border-color: #bfdbfe;
  color: var(--cn-primary);
}

.category-pill.active {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: var(--cn-primary);
  font-weight: 500;
}

.pill-count {
  font-size: 11px;
  color: #94a3b8;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-card {
  padding: 18px;
  cursor: pointer;
  transition: box-shadow 0.16s ease, transform 0.16s ease;
}

.knowledge-card:hover {
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.07);
  transform: translateY(-1px);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.view-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.knowledge-card h2 {
  margin: 12px 0 8px;
  font-size: 16px;
  line-height: 1.5;
}

.knowledge-card p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--cn-muted);
  min-height: 44px;
}

.card-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}

@media (max-width: 820px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
