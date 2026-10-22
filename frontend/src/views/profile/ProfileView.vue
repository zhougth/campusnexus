<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'

import { updatePassword, updateProfile, uploadAvatar } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import {
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'

const router = useRouter()
const auth = useAuthStore()

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const savingProfile = ref(false)
const savingPassword = ref(false)
const uploadingAvatar = ref(false)

const profileForm = reactive({
  username: '',
  email: '',
  phone: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: FormRules = {
  username: [{ min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const initial = computed(() => auth.user?.username?.slice(0, 1).toUpperCase() ?? 'U')

function syncForm(): void {
  if (auth.user) {
    profileForm.username = auth.user.username
    profileForm.email = auth.user.email ?? ''
    profileForm.phone = auth.user.phone ?? ''
  }
}

async function saveProfile(): Promise<void> {
  if (!profileFormRef.value) return
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingProfile.value = true
  try {
    await updateProfile({
      username: profileForm.username.trim() || undefined,
      email: profileForm.email.trim() || undefined,
      phone: profileForm.phone.trim() || undefined,
    })
    await auth.fetchCurrentUser()
    syncForm()
    ElMessage.success('资料已更新')
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    savingProfile.value = false
  }
}

async function savePassword(): Promise<void> {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingPassword.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    auth.clearSession()
    await router.replace('/login')
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    savingPassword.value = false
  }
}

async function handleAvatarUpload(options: UploadRequestOptions): Promise<void> {
  uploadingAvatar.value = true
  try {
    const url = await uploadAvatar(options.file)
    auth.updateLocalUser({ avatar: url })
    ElMessage.success('头像已更新')
  } catch (error) {
    if (error instanceof BackendNotImplementedError) {
      ElMessage.warning(`后端接口尚未实现：${error.method} ${error.path}`)
    } else if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    uploadingAvatar.value = false
  }
}

onMounted(async () => {
  await auth.ensureUser()
  syncForm()
})
</script>

<template>
  <div class="page-container narrow">
    <div class="page-header">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理你的账号资料、头像与密码</p>
      </div>
    </div>

    <div class="surface surface-pad profile-head">
      <el-avatar :size="72" :src="auth.user?.avatar ?? undefined">{{ initial }}</el-avatar>
      <div class="profile-info">
        <div class="name-row">
          <strong>{{ auth.user?.username ?? '—' }}</strong>
          <el-tag size="small" :type="auth.isAdmin ? 'danger' : 'info'" effect="plain">
            {{ auth.isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
        </div>
        <div class="text-muted text-small">
          注册于 {{ auth.user?.createTime ?? '—' }} · ID {{ auth.user?.id ?? '—' }}
        </div>
      </div>
      <el-upload
        :show-file-list="false"
        accept="image/jpeg,image/png,image/gif,image/webp"
        :http-request="handleAvatarUpload"
        class="avatar-upload"
      >
        <el-button :loading="uploadingAvatar">更换头像</el-button>
      </el-upload>
    </div>

    <div class="grid">
      <div class="surface surface-pad">
        <h2 class="section-title">基本资料</h2>
        <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="profileForm.username" maxlength="20" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="name@campus.edu" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="11 位手机号" />
          </el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
        </el-form>
      </div>

      <div class="surface surface-pad">
        <h2 class="section-title">修改密码</h2>
        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-button type="primary" :loading="savingPassword" @click="savePassword">修改密码</el-button>
          <p class="form-tip">修改成功后所有已签发的 Token 立即失效，需要重新登录。</p>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.narrow {
  max-width: 1000px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 18px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
}

@media (max-width: 820px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .profile-head {
    flex-wrap: wrap;
  }
}
</style>
