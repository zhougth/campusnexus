export function formatConfidence(confidence: string | null): string {
  if (confidence === 'high') return '高可信'
  if (confidence === 'medium') return '中可信'
  if (confidence === 'low') return '低可信'
  return '—'
}

export function confidenceTagType(confidence: string | null): 'success' | 'warning' | 'info' {
  if (confidence === 'high') return 'success'
  if (confidence === 'medium') return 'warning'
  return 'info'
}

export function formatSource(source: string | null): string {
  if (source === 'rag_knowledge_base') return '知识库'
  if (source === 'ai_generated') return 'AI 生成'
  return '—'
}

export function vectorStatusText(status: number): string {
  if (status === 1) return '已同步'
  if (status === 2) return '同步失败'
  return '待同步'
}

export function vectorStatusTagType(status: number): 'success' | 'danger' | 'warning' {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

export function contributionStatusText(status: number): string {
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '待审核'
}

export function contributionStatusTagType(status: number): 'success' | 'danger' | 'warning' {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

export function contributionTypeText(type: string): string {
  if (type === 'qa') return '问答'
  if (type === 'article') return '文章'
  if (type === 'correction') return '纠错'
  return type
}

export function feedbackTypeText(type: string): string {
  if (type === 'like') return '点赞'
  if (type === 'dislike') return '点踩'
  if (type === 'report') return '纠错'
  return type
}

export function taskStatusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '执行中',
    SUCCESS: '成功',
    PARTIAL: '部分成功',
    FAILED: '失败',
  }
  return map[status] ?? status
}

export function taskStatusTagType(status: string): 'info' | 'primary' | 'success' | 'warning' | 'danger' {
  if (status === 'SUCCESS') return 'success'
  if (status === 'PARTIAL') return 'warning'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'primary'
  return 'info'
}

export function batchStatusText(status: string): string {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    ROLLED_BACK: '已回滚',
  }
  return map[status] ?? status
}

export function batchStatusTagType(status: string): 'success' | 'danger' | 'info' {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

export function truncate(text: string | null, max = 60): string {
  if (!text) return '—'
  return text.length > max ? `${text.slice(0, max)}…` : text
}
