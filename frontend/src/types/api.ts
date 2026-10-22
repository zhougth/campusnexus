export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export interface UserVO {
  id: number
  username: string
  email: string | null
  avatar: string | null
  phone: string | null
  role: number
  status: number
  createTime: string
  updateTime: string
}

export type AdminUserVO = UserVO

export interface CategoryVO {
  id: number
  name: string
  sort: number
  knowledgeCount: number
}

export interface AdminCategoryVO extends CategoryVO {
  status: number
  createTime: string
  updateTime: string
}

export interface KnowledgeVO {
  id: number
  categoryId: number
  categoryName: string
  title: string | null
  question: string | null
  answer: string | null
  keywords: string | null
  viewCount: number
  createTime: string
  status: number
  sourceType: string
  fileUrl: string | null
  originalFilename: string | null
  fileType: string | null
}

export interface AdminKnowledgeVO extends KnowledgeVO {
  vectorStatus: number
  vectorError: string | null
  vectorSyncedTime: string | null
  importBatchId: string | null
}

export interface HotTopicVO {
  id: number
  title: string
  keywords: string
  viewCount: number
}

export interface StatsVO {
  totalKnowledge: number
  todayQuestions: number
  totalContributions: number
  totalUsers: number
}

export interface DashboardVO {
  totalKnowledge: number
  todayQuestions: number
  answerRate: number
  totalUsers: number
  pendingContributions: number
  hotCategories: Array<{ name: string; count: number }>
}

export interface ConversationSessionVO {
  sessionId: string
  title: string
  messageCount: number
  startTime: string
  endTime: string | null
}

export interface ConversationMessage {
  id: number
  sessionId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  messageIndex: number
  source: string | null
  createTime: string
}

export interface ContributionVO {
  id: number
  title: string
  content: string
  type: 'qa' | 'article' | 'correction'
  categoryId: number
  status: number
  remark: string | null
  createTime: string
}

export interface AdminContributionVO extends ContributionVO {
  userId: number
  username: string
  submitRemark: string | null
  knowledgeId: number | null
}

export interface FeedbackVO {
  id: number
  userId: number
  logId: number
  knowledgeId: number | null
  feedbackType: 'like' | 'dislike' | 'report'
  content: string | null
  status: number
  sessionId: string | null
  correctAnswer: string | null
  createdContributionId: number | null
  reviewedBy: number | null
  reviewedTime: string | null
  reviewRemark: string | null
  createTime: string
}

export interface QuestionLogVO {
  id: number
  userId: number | null
  username: string | null
  sessionId: string
  question: string
  answerContent: string | null
  confidence: string | null
  source: string | null
  matchCount: number
  citations: Array<{ knowledgeId: number; title: string }> | null
  createTime: string
}

export interface QuestionLogDetailVO extends QuestionLogVO {
  feedbacks: FeedbackVO[] | null
}

export interface ImportBatchVO {
  batchId: string
  originalFilename: string
  fileType: string
  fileUrl: string
  categoryId: number
  categoryName: string
  keywords: string | null
  knowledgeCount: number
  totalChars: number
  status: 'SUCCESS' | 'FAILED' | 'ROLLED_BACK'
  errorMessage: string | null
  createdBy: number
  createTime: string
  rolledBackBy: number | null
  rolledBackTime: string | null
}

export interface ImportBatchDetailVO extends ImportBatchVO {
  vectorSyncedCount: number
  vectorPendingCount: number
  vectorFailedCount: number
}

export type TaskType = 'VECTORIZE' | 'BATCH_ROLLBACK'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL' | 'FAILED'

export interface AsyncTaskVO {
  taskId: string
  taskType: TaskType
  status: TaskStatus
  total: number
  processed: number
  success: number
  failed: number
  progress: number
  payload: Record<string, unknown> | null
  errorMessage: string | null
  createdBy: number
  startedAt: string | null
  finishedAt: string | null
  createTime: string
}

export interface DocumentUploadResponse {
  batchId: string
  originalFilename: string
  fileType: string
  fileUrl: string
  docTitle: string | null
  knowledgeCount: number
  totalChars: number
}

export interface LoginDTO {
  username: string
  password: string
}

export interface RegisterDTO {
  username: string
  password: string
  email?: string
}

export interface UpdateProfileDTO {
  username?: string
  email?: string
  phone?: string
}

export interface UpdatePasswordDTO {
  oldPassword: string
  newPassword: string
}

export interface ContributionSubmitDTO {
  title: string
  content: string
  type: 'qa' | 'article' | 'correction'
  categoryId: number
  remark?: string
}

export interface FeedbackSubmitDTO {
  logId: number
  feedbackType: 'like' | 'dislike' | 'report'
  knowledgeId?: number
  sessionId?: string
  content?: string
  correctAnswer?: string
}

export interface KnowledgeImportDTO {
  categoryId: number
  title?: string
  question?: string
  answer?: string
  keywords?: string
}

export type KnowledgeUpdateDTO = Partial<KnowledgeImportDTO>

export interface CategoryCreateDTO {
  name: string
  sort?: number
  status?: number
}

export interface CategoryUpdateDTO {
  name?: string
  sort?: number
  status?: number
}

export interface FeedbackProcessDTO {
  remark?: string
}

export interface ContributionReviewDTO {
  status: 1 | 2
  remark?: string
  categoryId?: number
}

export interface BatchRollbackDTO {
  force?: boolean
  deleteFile?: boolean
}

export interface VectorizeResponse {
  taskId: string
  status: TaskStatus
  pendingCount: number
}

export interface PageQuery {
  page?: number
  size?: number
}

export interface KnowledgeQuery extends PageQuery {
  keyword?: string
  categoryId?: number
  status?: number
  importBatchId?: string
}

export interface ContributionQuery extends PageQuery {
  status?: number
}

export interface FeedbackQuery extends PageQuery {
  feedbackType?: string
  status?: number
}

export interface UserQuery extends PageQuery {
  keyword?: string
  role?: number
  status?: number
}

export interface TaskQuery extends PageQuery {
  taskType?: TaskType
  status?: TaskStatus
}

export interface QuestionLogQuery extends PageQuery {
  userId?: number
  sessionId?: string
  keyword?: string
  source?: string
  confidence?: string
  startTime?: string
  endTime?: string
}

export interface ImportBatchQuery extends PageQuery {
  categoryId?: number
  status?: string
  startTime?: string
  endTime?: string
}

export type ChatEvent =
  | { type: 'status'; content: string }
  | { type: 'token'; content: string }
  | { type: 'followUp'; content: string }
  | {
      type: 'done'
      sessionId: string
      confidence: 'high' | 'medium' | 'low'
      source: 'rag_knowledge_base' | 'ai_generated'
      matchCount: number
      logId: number
      recommendations?: Array<{ knowledgeId: number; title: string; reason: string }>
    }
  | { type: 'error'; code: number; content: string }

export type ChatRole = 'user' | 'assistant' | 'system'
