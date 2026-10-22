import type {
  AdminCategoryVO,
  AdminContributionVO,
  AdminKnowledgeVO,
  AdminUserVO,
  AsyncTaskVO,
  BatchRollbackDTO,
  CategoryCreateDTO,
  CategoryUpdateDTO,
  ContributionReviewDTO,
  DashboardVO,
  DocumentUploadResponse,
  ImportBatchDetailVO,
  ImportBatchQuery,
  ImportBatchVO,
  KnowledgeImportDTO,
  KnowledgeQuery,
  KnowledgeUpdateDTO,
  PageResult,
  QuestionLogDetailVO,
  QuestionLogQuery,
  QuestionLogVO,
  TaskQuery,
  UserQuery,
  VectorizeResponse,
} from '@/types/api'
import { request } from '@/utils/http'

export function fetchDashboard(): Promise<DashboardVO> {
  return request({ url: '/admin/dashboard', method: 'GET' })
}

export function fetchAdminKnowledge(
  params: KnowledgeQuery,
): Promise<PageResult<AdminKnowledgeVO>> {
  return request({ url: '/admin/knowledge', method: 'GET', params })
}

export function createKnowledge(dto: KnowledgeImportDTO): Promise<void> {
  return request({ url: '/admin/knowledge', method: 'POST', data: dto })
}

export function uploadDocument(
  form: FormData,
  onProgress?: (percent: number) => void,
): Promise<DocumentUploadResponse> {
  return request({
    url: '/admin/knowledge/upload',
    method: 'POST',
    data: form,
    timeout: 120000,
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    },
  })
}

export function updateKnowledge(id: number, dto: KnowledgeUpdateDTO): Promise<void> {
  return request({ url: `/admin/knowledge/${id}`, method: 'PATCH', data: dto })
}

export function deleteKnowledge(id: number): Promise<void> {
  return request({ url: `/admin/knowledge/${id}`, method: 'DELETE' })
}

export function fetchAdminContributions(params: {
  page?: number
  size?: number
  status?: number
}): Promise<PageResult<AdminContributionVO>> {
  return request({ url: '/admin/contributions', method: 'GET', params })
}

export function fetchAdminContributionDetail(id: number): Promise<AdminContributionVO> {
  return request({ url: `/admin/contributions/${id}`, method: 'GET' })
}

export function reviewContribution(
  id: number,
  dto: ContributionReviewDTO,
): Promise<void> {
  return request({ url: `/admin/contributions/${id}/review`, method: 'PUT', data: dto })
}

export function triggerVectorize(): Promise<VectorizeResponse> {
  return request({ url: '/admin/knowledge/vectorize', method: 'POST', silent: true })
}

export function fetchTasks(params: TaskQuery): Promise<PageResult<AsyncTaskVO>> {
  return request({ url: '/admin/tasks', method: 'GET', params })
}

export function fetchTaskDetail(taskId: string): Promise<AsyncTaskVO> {
  return request({ url: `/admin/tasks/${taskId}`, method: 'GET' })
}

export function fetchUsers(params: UserQuery): Promise<PageResult<AdminUserVO>> {
  return request({ url: '/admin/users', method: 'GET', params })
}

export function fetchUserDetail(id: number): Promise<AdminUserVO> {
  return request({ url: `/admin/users/${id}`, method: 'GET' })
}

export function updateUserStatus(id: number, status: number): Promise<void> {
  return request({ url: `/admin/users/${id}/status`, method: 'PUT', data: { status } })
}

export function updateUserRole(id: number, role: number): Promise<void> {
  return request({ url: `/admin/users/${id}/role`, method: 'PUT', data: { role } })
}

export function fetchAdminCategories(params?: {
  status?: number
  keyword?: string
}): Promise<AdminCategoryVO[]> {
  return request({ url: '/admin/categories', method: 'GET', params })
}

export function createCategory(dto: CategoryCreateDTO): Promise<void> {
  return request({ url: '/admin/categories', method: 'POST', data: dto })
}

export function updateCategory(id: number, dto: CategoryUpdateDTO): Promise<void> {
  return request({ url: `/admin/categories/${id}`, method: 'PUT', data: dto })
}

export function deleteCategory(id: number): Promise<void> {
  return request({ url: `/admin/categories/${id}`, method: 'DELETE' })
}

export function fetchQuestionLogs(
  params: QuestionLogQuery,
): Promise<PageResult<QuestionLogVO>> {
  return request({ url: '/admin/question-logs', method: 'GET', params })
}

export function fetchQuestionLogDetail(id: number): Promise<QuestionLogDetailVO> {
  return request({ url: `/admin/question-logs/${id}`, method: 'GET' })
}

export function fetchImportBatches(
  params: ImportBatchQuery,
): Promise<PageResult<ImportBatchVO>> {
  return request({ url: '/admin/import-batches', method: 'GET', params })
}

export function fetchImportBatchDetail(batchId: string): Promise<ImportBatchDetailVO> {
  return request({ url: `/admin/import-batches/${batchId}`, method: 'GET' })
}

export function rollbackImportBatch(
  batchId: string,
  dto: BatchRollbackDTO,
): Promise<VectorizeResponse> {
  return request({
    url: `/admin/import-batches/${batchId}/rollback`,
    method: 'POST',
    data: dto,
    silent: true,
  })
}
