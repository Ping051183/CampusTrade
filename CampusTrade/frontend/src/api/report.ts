import request from '@/utils/request'
import type { ApiResult, Report } from './types'

/**
 * 举报API
 */
export const reportAPI = {
  /** 举报商品 */
  add(data: Report): Promise<ApiResult<null>> {
    return request.post('/report/add', data)
  }
}
