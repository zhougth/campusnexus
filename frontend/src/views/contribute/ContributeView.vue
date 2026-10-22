<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { fetchCategories } from '@/api/common'
import { submitContribution } from '@/api/contribute'
import type { CategoryVO } from '@/types/api'
import { BackendNotImplementedError, BackendUnreachableError } from '@/utils/errors'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const categories = ref<CategoryVO[]>([])
const categoriesUnavailable = ref(false)

const form = reactive({
  title: '',
  content: '',
  type: 'qa' as 'qa' | 'article' | 'correction',
  categoryId: null as number | null,
  remark: '',
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 字', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择贡献类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  remark: [{ max: 500, message: '补充说明不能超过 500 字', trigger: 'blur' }],
}

const contentPlaceholder = () => {
  if (form.type === 'qa') {
    return '请按“问：...\\n答：...”的格式填写问答内容'
  }
  if (form.type === 'correction') {
    return '请说明现有答案的问题，并给出正确的解答'
  }
  return '请填写完整的知识内容'
}

function loadCategories(): void {
  void fetchCategories()
    .then((list) => {
      categories.value = list
    })
    .catch((error) => {
      categoriesUnavailable.value = true
      if (error instanceof BackendNotImplementedError || error instanceof BackendUnreachableError) {
        ElMessage.warning('分类接口暂不可用，后端完成后再提交贡献')
      }
    })
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || form.categoryId === null) return

  submitting.value = true
  try {
    await submitContribution({
      title: form.title.trim(),
      content: form.content.trim(),
      type: form.type,
      categoryId: form.categoryId,
      remark: form.remark.trim() || undefined,
    })
    ElMessage.success('贡献已提交，等待管理员审核')
    formRef.value.resetFields()
    form.type = 'qa'
    form.categoryId = null
  } catch (error) {
    if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadCategories)
</script>

<template>
  <div class="page-container narrow">
    <div class="page-header">
      <div>
        <h1 class="page-title">提交贡献</h1>
        <p class="page-subtitle">分享你了解的校园知识，审核通过后会进入知识库</p>
      </div>
      <el-button @click="router.push('/my/contributions')">我的贡献</el-button>
    </div>

    <div class="surface surface-pad">
      <el-alert
        v-if="categoriesUnavailable"
        type="warning"
        :closable="false"
        show-icon
        title="分类接口暂不可用"
        description="贡献提交依赖分类数据，请等待后端接口完成后再提交。"
        class="mb"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="贡献类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button value="qa">问答</el-radio-button>
            <el-radio-button value="article">文章</el-radio-button>
            <el-radio-button value="correction">纠错</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-select
            v-model="form.categoryId"
            placeholder="请选择分类"
            style="width: 100%"
            :disabled="categories.length === 0"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="一句话概括这条知识" />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            :placeholder="contentPlaceholder()"
          />
          <div v-if="form.type === 'qa'" class="form-tip">
            问答类型请使用“问：问题 / 答：答案”格式，审核通过后可直接进入知识库。
          </div>
        </el-form-item>

        <el-form-item label="补充说明（可选）" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="给审核员的说明，例如资料来源"
          />
        </el-form-item>

        <div class="actions">
          <el-button type="primary" :loading="submitting" @click="submit">提交贡献</el-button>
          <span class="text-muted text-small">提交后可在「我的贡献」查看审核进度</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.narrow {
  max-width: 900px;
}

.mb {
  margin-bottom: 18px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 8px;
}
</style>
