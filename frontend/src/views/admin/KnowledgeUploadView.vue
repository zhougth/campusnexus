<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadFile, type UploadUserFile } from 'element-plus'

import { fetchImportBatchDetail, uploadDocument } from '@/api/admin'
import { fetchCategories } from '@/api/common'
import type { CategoryVO, DocumentUploadResponse } from '@/types/api'
import {
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'
import { batchStatusTagType, batchStatusText } from '@/utils/format'

const router = useRouter()

const categories = ref<CategoryVO[]>([])
const categoryId = ref<number | null>(null)
const keywords = ref('')
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const processing = ref(false)
const progress = ref(0)
const result = ref<DocumentUploadResponse | null>(null)
const errorMessage = ref('')

const accept = '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.markdown,.html,.htm,.csv'

let pollCancelled = false
let pollTimer: number | null = null

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

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    pollTimer = window.setTimeout(resolve, ms)
  })
}

function stopPolling(): void {
  pollCancelled = true
  if (pollTimer !== null) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
}

async function pollBatchResult(batchId: string): Promise<void> {
  if (pollCancelled) {
    return
  }
  processing.value = true
  const deadline = Date.now() + 10 * 60 * 1000

  try {
    while (!pollCancelled && Date.now() < deadline) {
      await sleep(2000)
      if (pollCancelled) {
        return
      }

      const detail = await fetchImportBatchDetail(batchId)
      const status = detail.status === 'ROLLED_BACK' ? 'FAILED' : detail.status
      if (result.value) {
        result.value = {
          ...result.value,
          knowledgeCount: detail.knowledgeCount,
          totalChars: detail.totalChars,
          status,
        }
      }

      if (detail.status === 'SUCCESS') {
        ElMessage.success(`导入完成，共生成 ${detail.knowledgeCount} 条知识`)
        return
      }
      if (detail.status === 'FAILED') {
        errorMessage.value = detail.errorMessage || '文档导入失败'
        return
      }
      if (detail.status === 'ROLLED_BACK') {
        errorMessage.value = '该导入批次已回滚'
        return
      }
    }

    if (!pollCancelled) {
      errorMessage.value = '导入处理时间较长，请到导入批次页查看最终结果'
    }
  } catch (error) {
    if (error instanceof BackendNotImplementedError) {
      errorMessage.value = `后端接口尚未实现：${error.method} ${error.path}`
    } else if (error instanceof BackendUnreachableError) {
      errorMessage.value = '无法连接后端服务，请到导入批次页确认处理结果'
    } else if (error instanceof Error) {
      errorMessage.value = error.message
    }
  } finally {
    processing.value = false
  }
}

async function submit(): Promise<void> {
  if (uploading.value || processing.value) {
    return
  }
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

    const response = await uploadDocument(form, (percent) => {
      progress.value = percent
    })
    result.value = response
    fileList.value = []
    selectedFile.value = null
    ElMessage.success('文件已上传，正在后台解析并生成知识条目')
    uploading.value = false
    await pollBatchResult(response.batchId)
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
onUnmounted(stopPolling)
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
          <el-select
            v-model="categoryId"
            placeholder="请选择分类"
            style="width: 320px"
            :disabled="uploading || processing"
          >
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="补充关键词（可选，英文逗号分隔）">
          <el-input
            v-model="keywords"
            maxlength="500"
            show-word-limit
            placeholder="例如：手册,新生,指南"
            :disabled="uploading || processing"
          />
        </el-form-item>

        <el-form-item label="文档文件">
          <el-upload
            v-model:file-list="fileList"
            drag
            :auto-upload="false"
            :limit="1"
            :accept="accept"
            :disabled="uploading || processing"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onExceed"
            class="uploader"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择文件</em></div>
            <template #tip>
              <div class="el-upload__tip">
                上传后在后台解析并生成知识条目，向量状态为「待同步」，需在知识管理中手动同步向量。
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <div v-if="uploading" class="progress-row">
          <el-progress :percentage="progress" :stroke-width="8" />
        </div>

        <div class="actions">
          <el-button
            type="primary"
            :loading="uploading || processing"
            :disabled="uploading || processing"
            @click="submit"
          >
            {{ processing ? '正在生成知识条目' : '上传并入库' }}
          </el-button>
          <span class="text-muted text-small">文件上传完成后，解析和生成会在后台继续执行</span>
        </div>
      </el-form>
    </div>

    <el-alert
      v-if="errorMessage"
      class="mt-16"
      type="error"
      :closable="false"
      show-icon
      title="上传或导入失败"
      :description="errorMessage"
    />

    <div v-if="result" class="surface surface-pad mt-16">
      <h2 class="result-title">
        <template v-if="result.status === 'PROCESSING'">正在导入</template>
        <template v-else-if="result.status === 'SUCCESS'">导入完成</template>
        <template v-else>导入失败</template>
      </h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="批次 ID">
          <span class="mono">{{ result.batchId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="batchStatusTagType(result.status)" effect="plain">
            {{ batchStatusText(result.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件名">{{ result.originalFilename }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ result.fileType }}</el-descriptions-item>
        <el-descriptions-item label="解析标题">{{ result.docTitle || '—' }}</el-descriptions-item>
        <el-descriptions-item label="生成知识数">{{ result.knowledgeCount }}</el-descriptions-item>
        <el-descriptions-item label="文档字符数">{{ result.totalChars }}</el-descriptions-item>
      </el-descriptions>

      <div class="result-actions">
        <el-alert
          v-if="result.status === 'PROCESSING'"
          type="info"
          :closable="false"
          show-icon
          title="文件已上传，后台正在解析并生成知识条目"
          description="页面会自动刷新批次状态，也可以离开本页，稍后到导入批次中查看。"
        />
        <el-alert
          v-else-if="result.status === 'SUCCESS'"
          type="warning"
          :closable="false"
          show-icon
          title="新知识尚未同步向量，只能被关键词检索命中"
          description="请到知识管理点击「同步向量」，或在异步任务页查看进度。"
        />
        <div class="toolbar mt-16">
          <el-button @click="router.push('/admin/import-batches')">
            查看导入批次
          </el-button>
          <el-button v-if="result.status === 'SUCCESS'" type="primary" @click="router.push('/admin/knowledge')">
            前往知识管理
          </el-button>
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
