import request from '@/utils/request'
import type { ApiResult, PageResult, Notification } from './types'

/**
 * 系统通知API
 */
export const notificationAPI = {
  /** 我的通知列表 */
  list(params: {
    page?: number
    pageSize?: number
    unreadOnly?: boolean
  }): Promise<ApiResult<PageResult<Notification>>> {
    return request.get('/notification/list', { params })
  },

  /** 未读通知数 */
  unreadCount(): Promise<ApiResult<number>> {
    return request.get('/notification/unread-count')
  },

  /** 标记单条已读 */
  markRead(id: number): Promise<ApiResult<null>> {
    return request.put(`/notification/read/${id}`)
  },

  /** 全部标记已读 */
  markAllRead(): Promise<ApiResult<null>> {
    return request.put('/notification/read-all')
  }
}