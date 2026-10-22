<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()

const menuItems = [
  { path: '/admin/dashboard', label: '仪表盘', icon: 'Odometer' },
  { path: '/admin/knowledge', label: '知识管理', icon: 'Notebook' },
  { path: '/admin/knowledge/upload', label: '文档上传', icon: 'UploadFilled' },
  { path: '/admin/contributions', label: '贡献审核', icon: 'CircleCheck' },
  { path: '/admin/feedback', label: '反馈处理', icon: 'ChatLineSquare' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/categories', label: '分类管理', icon: 'Grid' },
  { path: '/admin/question-logs', label: '提问日志', icon: 'Document' },
  { path: '/admin/import-batches', label: '导入批次', icon: 'FolderOpened' },
  { path: '/admin/tasks', label: '异步任务', icon: 'Timer' },
]

const activeMenu = computed(() => {
  const matched = menuItems
    .filter((item) => route.path === item.path || route.path.startsWith(`${item.path}/`))
    .sort((a, b) => b.path.length - a.path.length)
  return matched[0]?.path ?? route.path
})
</script>

<template>
  <div class="admin-layout">
    <AppHeader />
    <div class="admin-body">
      <aside class="admin-aside">
        <div class="aside-title">管理后台</div>
        <el-menu :default-active="activeMenu" router class="admin-menu">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </aside>
      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.admin-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.admin-aside {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--cn-border);
  min-height: calc(100vh - 64px);
  padding: 16px 10px;
}

.aside-title {
  padding: 4px 12px 12px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--cn-muted);
}

.admin-menu {
  border-right: none;
}

.admin-menu :deep(.el-menu-item) {
  height: 42px;
  border-radius: 8px;
  margin-bottom: 4px;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: #eff6ff;
  color: var(--cn-primary);
}

.admin-main {
  flex: 1;
  min-width: 0;
}

@media (max-width: 900px) {
  .admin-aside {
    width: 64px;
    padding: 12px 6px;
  }

  .aside-title {
    display: none;
  }

  .admin-menu :deep(.el-menu-item span) {
    display: none;
  }
}
</style>
