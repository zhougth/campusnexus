import type {
  FeedbackProcessDTO,
  FeedbackQuery,
  FeedbackSubmitDTO,
  FeedbackVO,
  PageResult,
} from '@/types/api'
import { request } from '@/utils/http'

export function submitFeedback(dto: FeedbackSubmitDTO): Promise<void> {
  return request({ url: '/feedback/submit', method: 'POST', data: dto })
}

export function fetchFeedbackList(
  params: FeedbackQuery,
): Promise<PageResult<FeedbackVO>> {
  return request({ url: '/feedback/admin/list', method: 'GET', params })
}

export function processFeedback(
  feedbackId: number,
  dto: FeedbackProcessDTO,
): Promise<void> {
  return request({
    url: `/feedback/admin/process/${feedbackId}`,
    method: 'POST',
    data: dto,
  })
}
