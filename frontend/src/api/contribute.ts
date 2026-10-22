import type {
  ContributionQuery,
  ContributionSubmitDTO,
  ContributionVO,
  PageResult,
} from '@/types/api'
import { request } from '@/utils/http'

export function submitContribution(dto: ContributionSubmitDTO): Promise<void> {
  return request({ url: '/contribute', method: 'POST', data: dto })
}

export function fetchMyContributions(
  params: ContributionQuery,
): Promise<PageResult<ContributionVO>> {
  return request({ url: '/contribute/my', method: 'GET', params })
}
