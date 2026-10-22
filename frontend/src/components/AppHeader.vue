<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const links = computed(() => {
  const base = [
    { label: '智能问答', path: '/' },
    { label: '知识库', path: '/knowledge' },
    { label: '提交贡献', path: '/contribute', auth: true },
    { label: '我的贡献', path: '/my/contributions', auth: true },
  ]
  return base.filter((item) => !item.auth || auth.isLoggedIn)
})

function isActive(path: string): boolean {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}

async function handleCommand(command: string): Promise<void> {
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    if (route.meta.requiresAuth) {
      router.push('/login')
    }
    return
  }
  router.push(command)
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/" class="brand">
        <span class="brand-mark">C</span>
        <span class="brand-text">
          <strong>CampusNexus</strong>
          <small>校园生活百事通</small>
        </span>
      </router-link>

      <nav class="nav">
        <router-link
          v-for="link in links"
          :key="link.path"
          :to="link.path"
          class="nav-link"
          :class="{ active: isActive(link.path) }"
        >
          {{ link.label }}
        </router-link>
        <router-link
          v-if="auth.isAdmin"
          to="/admin/dashboard"
          class="nav-link"
          :class="{ active: route.path.startsWith('/admin') }"
        >
          管理后台
        </router-link>
      </nav>

      <div class="account">
        <template v-if="auth.isLoggedIn">
          <el-dropdown trigger="click" @command="handleCommand">
            <button type="button" class="account-button">
              <el-avatar :size="30" :src="auth.user?.avatar ?? undefined">
                {{ auth.user?.username?.slice(0, 1).toUpperCase() ?? 'U' }}
              </el-avatar>
              <span class="account-name">{{ auth.user?.username ?? '我的账号' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="/my/contributions">我的贡献</el-dropdown-item>
                <el-dropdown-item v-if="auth.isAdmin" command="/admin/dashboard">
                  管理后台
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button text @click="router.push('/login')">登录</el-button>
          <el-button type="primary" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--cn-border);
}

.header-inner {
  max-width: 1360px;
  height: 64px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 700;
  font-size: 18px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-size: 15px;
}

.brand-text small {
  font-size: 11px;
  color: var(--cn-muted);
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  overflow-x: auto;
}

.nav-link {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #4b5563;
  white-space: nowrap;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-link:hover {
  background: #f1f5f9;
  color: var(--cn-text);
}

.nav-link.active {
  background: #eff6ff;
  color: var(--cn-primary);
  font-weight: 500;
}

.account {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.account-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  color: var(--cn-text);
}

.account-button:hover {
  background: #f1f5f9;
}

.account-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

@media (max-width: 860px) {
  .header-inner {
    gap: 12px;
    padding: 0 14px;
  }

  .brand-text small,
  .account-name {
    display: none;
  }
}
</style>
