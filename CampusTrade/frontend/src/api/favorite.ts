import request from '@/utils/request'
import type { ApiResult, Goods } from './types'

/**
 * 收藏API
 */
export const favoriteAPI = {
  /** 添加收藏 */
  add(goodsId: number): Promise<ApiResult<null>> {
    return request.post(`/favorite/add/${goodsId}`)
  },

  /** 取消收藏 */
  cancel(goodsId: number): Promise<ApiResult<null>> {
    return request.delete(`/favorite/cancel/${goodsId}`)
  },

  /** 我的收藏 */
  getMyFavorites(): Promise<ApiResult<Goods[]>> {
    return request.get('/favorite/my')
  },

  /** 检查是否已收藏 */
  check(goodsId: number): Promise<ApiResult<boolean>> {
    return request.get(`/favorite/check/${goodsId}`)
  }
}
