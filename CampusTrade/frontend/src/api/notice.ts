import request from '@/utils/request'
import type { ApiResult, Notice } from './types'

/**
 * 公告API
 */
export const noticeAPI = {
  /** 获取最新公告列表 */
  getList(): Promise<ApiResult<Notice[]>> {
    return request.get('/notice/list')
  }
}
