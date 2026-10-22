import type {
  ConversationMessage,
  ConversationSessionVO,
  PageResult,
} from '@/types/api'
import { request } from '@/utils/http'

export function createSession(question: string): Promise<ConversationSessionVO> {
  return request({ url: '/chat/session', method: 'POST', params: { question }, silent: true })
}

export function fetchSessions(
  page = 1,
  size = 20,
): Promise<PageResult<ConversationSessionVO>> {
  return request({ url: '/chat/sessions', method: 'GET', params: { page, size }, silent: true })
}

export function fetchHistory(
  sessionId: string,
  limit = 50,
  beforeMessageIndex?: number,
): Promise<ConversationMessage[]> {
  return request({
    url: '/chat/history',
    method: 'GET',
    params: { sessionId, limit, beforeMessageIndex },
    silent: true,
  })
}
