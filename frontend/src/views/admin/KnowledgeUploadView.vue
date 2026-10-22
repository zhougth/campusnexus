<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadFile, type UploadUserFile } from 'element-plus'

import { uploadDocument } from '@/api/admin'
import { fetchCategories } from '@/api/common'
import type { CategoryVO, DocumentUploadResponse } from '@/types/api'
import {
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'

const router = useRouter()

const categories = ref<CategoryVO[]>([])
const categoryId = ref<number | null>(null)
const keywords = ref('')
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const progress = ref(0)
const result = ref<DocumentUploadResponse | null>(null)
const errorMessage = ref('')

const accept = '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.markdown,.html,.htm,.csv'

function loadCategories(): void {
  void fetchCategories()
    .then((list) => {
      categories.value = list
    })
    .catch(() => {
      categories.value = []
    })
}

function onFileChange(file: UploadFile): void {
  selectedFile.value = file.raw ?? null
  result.value = null
  errorMessage.value = ''
}

function onFileRemove(): void {
  selectedFile.value = null
}

function onExceed(): void {
  ElMessage.warning('一次只能上传一个文件，请先移除已选文件')
}

async function submit(): Promise<void> {
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的文档')
    return
  }
  if (categoryId.value === null) {
    ElMessage.warning('请选择目标分类')
    return
  }
  if (selectedFile.value.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 20MB')
    return
  }

  uploading.value = true
  progress.value = 0
  result.value = null
  errorMessage.value = ''
  try {
    const form = new FormData()
    form.append('file', selectedFile.value)
    form.append('categoryId', String(categoryId.value))
    if (keywords.value.trim()) {
      form.append('keywords', keywords.value.trim())
    }
    result.value = await uploadDocument(form, (percent) => {
      progress.value = percent
    })
    ElMessage.success('文档已入库，记得同步向量')
    fileList.value = []
    selectedFile.value = null
  } catch (error) {
    if (error instanceof BackendNotImplementedError) {
      errorMessage.value = `后端接口尚未实现：${error.method} ${error.path}`
    } else if (error instanceof BackendUnreachableError) {
      errorMessage.value = '无法连接后端服务，请确认 Spring Boot 已启动'
    } else if (error instanceof Error) {
      errorMessage.value = error.message
    }
  } finally {
    uploading.value = false
  }
}

onMounted(loadCategories)
</script>

<template>
  <div class="page-container narrow">
    <div class="page-header">
      <div>
        <h1 class="page-title">文档上传</h1>
        <p class="page-subtitle">支持 PDF / Word / Excel / PPT / TXT / Markdown / HTML / CSV，单文件 ≤ 20MB</p>
      </div>
      <el-button @click="router.push('/admin/import-batches')">导入批次</el-button>
    </div>

    <div class="surface surface-pad">
      <el-form label-position="top">
        <el-form-item label="目标分类（必填）">
          <el-select v-model="categoryId" placeholder="请选择分类" style="width: 320px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="补充关键词（可选，英文逗号分隔）">
          <el-input v-model="keywords" maxlength="500" show-word-limit placeholder="例如：手册,新生,指南" />
        </el-form-item>

        <el-form-item label="文档文件">
          <el-upload
            v-model:file-list="fileList"
            drag
            :auto-upload="false"
            :limit="1"
            :accept="accept"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onExceed"
            class="uploader"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择文件</em></div>
            <template #tip>
              <div class="el-upload__tip">
                上传后会在后台解析切分并生成知识条目，向量状态为「待同步」，需在知识管理中手动同步向量。
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <div v-if="uploading" class="progress-row">
          <el-progress :percentage="progress" :stroke-width="8" />
        </div>

        <div class="actions">
          <el-button type="primary" :loading="uploading" @click="submit">上传并入库</el-button>
          <span class="text-muted text-small">长文档解析可能需要一段时间</span>
        </div>
      </el-form>
    </div>

    <el-alert
      v-if="errorMessage"
      class="mt-16"
      type="error"
      :closable="false"
      show-icon
      title="上传失败"
      :description="errorMessage"
    />

    <div v-if="result" class="surface surface-pad mt-16">
      <h2 class="result-title">上传成功</h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="批次 ID">
          <span class="mono">{{ result.batchId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="文件名">{{ result.originalFilename }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ result.fileType }}</el-descriptions-item>
        <el-descriptions-item label="解析标题">{{ result.docTitle || '—' }}</el-descriptions-item>
        <el-descriptions-item label="生成知识数">{{ result.knowledgeCount }}</el-descriptions-item>
        <el-descriptions-item label="文档字符数">{{ result.totalChars }}</el-descriptions-item>
      </el-descriptions>
      <div class="result-actions">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="新知识尚未同步向量，只能被关键词检索命中"
          description="请到知识管理点击「同步向量」，或在异步任务页查看进度。"
        />
        <div class="toolbar mt-16">
          <el-button @click="router.push('/admin/tasks')">查看异步任务</el-button>
          <el-button type="primary" @click="router.push('/admin/knowledge')">前往知识管理</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.narrow {
  max-width: 960px;
}

.uploader {
  width: 100%;
}

.progress-row {
  margin: 4px 0 16px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.result-title {
  margin: 0 0 16px;
  font-size: 16px;
}
</style>
