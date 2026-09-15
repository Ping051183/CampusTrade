import request from '@/utils/request'
import type { ApiResult, Comment } from './types'

/**
 * 评论API
 */
export const commentAPI = {
  /** 获取评论列表 */
  getList(goodsId: number): Promise<ApiResult<Comment[]>> {
    return request.get(`/comment/list/${goodsId}`)
  },

  /** 添加评论 */
  add(data: Comment): Promise<ApiResult<null>> {
    return request.post('/comment/add', data)
  },

  /** 删除评论 */
  delete(id: number): Promise<ApiResult<null>> {
    return request.delete(`/comment/delete/${id}`)
  }
}
