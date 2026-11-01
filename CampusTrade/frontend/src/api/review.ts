import request from '@/utils/request'
import type { ApiResult, PageResult, Review, ReviewSummary } from './types'

/**
 * 交易评价API
 */
export const reviewAPI = {
  /** 发表评价 */
  create(data: { orderId: number; score: number; content?: string }): Promise<ApiResult<null>> {
    return request.post('/review', data)
  },

  /** 查询某个用户收到的评价 */
  pageByUser(
    userId: number,
    params: { page?: number; pageSize?: number }
  ): Promise<ApiResult<PageResult<Review>>> {
    return request.get(`/review/user/${userId}`, { params })
  },

  /** 查询某个用户的信用汇总 */
  summary(userId: number): Promise<ApiResult<ReviewSummary>> {
    return request.get(`/review/summary/${userId}`)
  }
}