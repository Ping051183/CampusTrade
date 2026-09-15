import request from '@/utils/request'
import type { ApiResult, Category } from './types'

/**
 * 分类API
 */
export const categoryAPI = {
  /** 获取所有分类 */
  getList(): Promise<ApiResult<Category[]>> {
    return request.get('/category/list')
  }
}
