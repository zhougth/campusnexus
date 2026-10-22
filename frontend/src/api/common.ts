import type {
  CategoryVO,
  HotTopicVO,
  KnowledgeVO,
  PageResult,
  StatsVO,
} from '@/types/api'
import { request } from '@/utils/http'

export function fetchHotTopics(limit = 10): Promise<HotTopicVO[]> {
  return request({ url: '/common/hot-topics', method: 'GET', params: { limit } })
}

export function clickHotTopic(id: number): Promise<void> {
  return request({ url: `/common/hot-topics/${id}/click`, method: 'POST', silent: true })
}

export function searchKnowledge(params: {
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<KnowledgeVO>> {
  return request({ url: '/common/search', method: 'GET', params })
}

export function searchKnowledgeByCategory(params: {
  categoryId: number
  page?: number
  size?: number
}): Promise<PageResult<KnowledgeVO>> {
  return request({ url: '/common/search/category', method: 'GET', params })
}

export function fetchKnowledgeDetail(id: number): Promise<KnowledgeVO> {
  return request({ url: `/common/knowledge/${id}`, method: 'GET' })
}

export function fetchStats(): Promise<StatsVO> {
  return request({ url: '/common/stats', method: 'GET' })
}

export function fetchCategories(): Promise<CategoryVO[]> {
  return request({ url: '/common/categories', method: 'GET' })
}
