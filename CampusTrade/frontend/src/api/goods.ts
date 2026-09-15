import request from '@/utils/request'
import type { ApiResult, PageResult, Goods, GoodsVO } from './types'

/**
 * 商品API
 */
export const goodsAPI = {
  /** 发布商品 */
  publish(data: Goods): Promise<ApiResult<null>> {
    return request.post('/goods/publish', data)
  },

  /** 编辑商品 */
  update(data: Goods): Promise<ApiResult<null>> {
    return request.put('/goods/update', data)
  },

  /** 删除商品 */
  deleteGoods(id: number): Promise<ApiResult<null>> {
    return request.delete(`/goods/delete/${id}`)
  },

  /** 下架商品 */
  offline(id: number): Promise<ApiResult<null>> {
    return request.put(`/goods/offline/${id}`)
  },

  /** 商品详情 */
  getDetail(id: number): Promise<ApiResult<GoodsVO>> {
    return request.get(`/goods/detail/${id}`)
  },

  /** 商品列表 */
  getList(params: {
    page?: number
    pageSize?: number
    categoryId?: number
    keyword?: string
  }): Promise<ApiResult<PageResult<GoodsVO>>> {
    return request.get('/goods/list', { params })
  },

  /** 我的商品 */
  getMyGoods(status?: number): Promise<ApiResult<Goods[]>> {
    return request.get('/goods/my', { params: { status } })
  }
}
