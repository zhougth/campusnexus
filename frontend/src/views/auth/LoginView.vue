<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { useAuthStore } from '@/stores/auth'
import { BackendUnreachableError } from '@/utils/errors'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' },
  ],
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await auth.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    const target = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    const redirect = target.startsWith('/') ? target : '/'
    await router.replace(redirect)
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务，请确认后端已启动')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="brand-mark">C</span>
        <div>
          <h1>CampusNexus</h1>
          <p>校园生活百事通 · 知识库智能问答</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            size="large"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="submit">
          登录
        </el-button>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
      <div class="auth-hint">登录后可保存会话、提交反馈与贡献</div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 15% 20%, rgba(37, 99, 235, 0.12), transparent 45%),
    radial-gradient(circle at 85% 80%, rgba(56, 189, 248, 0.16), transparent 45%),
    var(--cn-bg);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border: 1px solid var(--cn-border);
  border-radius: 16px;
  padding: 36px 32px 28px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 700;
  font-size: 22px;
}

.auth-brand h1 {
  margin: 0;
  font-size: 20px;
}

.auth-brand p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--cn-muted);
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.auth-footer {
  margin-top: 20px;
  font-size: 13px;
  text-align: center;
  color: var(--cn-muted);
}

.auth-footer a {
  color: var(--cn-primary);
  font-weight: 500;
}

.auth-hint {
  margin-top: 14px;
  font-size: 12px;
  text-align: center;
  color: #9ca3af;
}
</style>
