import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true, title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guestOnly: true, title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        {
          path: '',
          name: 'chat',
          component: () => import('@/views/chat/ChatView.vue'),
          meta: { title: '智能问答' },
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/views/knowledge/KnowledgeListView.vue'),
          meta: { title: '知识库' },
        },
        {
          path: 'knowledge/:id',
          name: 'knowledge-detail',
          component: () => import('@/views/knowledge/KnowledgeDetailView.vue'),
          meta: { title: '知识详情' },
        },
        {
          path: 'contribute',
          name: 'contribute',
          component: () => import('@/views/contribute/ContributeView.vue'),
          meta: { requiresAuth: true, title: '提交贡献' },
        },
        {
          path: 'my/contributions',
          name: 'my-contributions',
          component: () => import('@/views/contribute/MyContributionsView.vue'),
          meta: { requiresAuth: true, title: '我的贡献' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/profile/ProfileView.vue'),
          meta: { requiresAuth: true, title: '个人中心' },
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '仪表盘' },
        },
        {
          path: 'knowledge',
          name: 'admin-knowledge',
          component: () => import('@/views/admin/KnowledgeManageView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '知识管理' },
        },
        {
          path: 'knowledge/upload',
          name: 'admin-knowledge-upload',
          component: () => import('@/views/admin/KnowledgeUploadView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '文档上传' },
        },
        {
          path: 'contributions',
          name: 'admin-contributions',
          component: () => import('@/views/admin/ContributionReviewView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '贡献审核' },
        },
        {
          path: 'feedback',
          name: 'admin-feedback',
          component: () => import('@/views/admin/FeedbackManageView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '反馈处理' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UserManageView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '用户管理' },
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/views/admin/CategoryManageView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '分类管理' },
        },
        {
          path: 'question-logs',
          name: 'admin-question-logs',
          component: () => import('@/views/admin/QuestionLogView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '提问日志' },
        },
        {
          path: 'import-batches',
          name: 'admin-import-batches',
          component: () => import('@/views/admin/ImportBatchView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '导入批次' },
        },
        {
          path: 'tasks',
          name: 'admin-tasks',
          component: () => import('@/views/admin/TaskView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '异步任务' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth) {
    if (!auth.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (to.meta.requiresAdmin) {
      const user = await auth.ensureUser()
      if (!user) {
        if (!auth.isLoggedIn) {
          return { path: '/login', query: { redirect: to.fullPath } }
        }
        ElMessage.error('无法获取当前用户信息，请稍后重试')
        return { path: '/' }
      }
      if (user.role !== 1) {
        ElMessage.error('无权限访问管理后台')
        return { path: '/' }
      }
    }
  }

  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { path: '/' }
  }

  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = title ? `${title} · CampusNexus` : 'CampusNexus · 校园生活百事通'
})

export default router
