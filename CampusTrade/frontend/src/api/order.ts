import request from '@/utils/request'
import type { ApiResult, PageResult, Order } from './types'

/**
 * 订单API
 */
export const orderAPI = {
  /** 创建订单 */
  create(goodsId: number, remark?: string): Promise<ApiResult<Order>> {
    return request.post(`/order/create/${goodsId}`, null, { params: { remark } })
  },

  /** 订单详情 */
  getDetail(id: number): Promise<ApiResult<Order>> {
    return request.get(`/order/detail/${id}`)
  },

  /** 我的购买订单 */
  getMyBuyOrders(params: { page?: number; pageSize?: number; status?: number }): Promise<ApiResult<PageResult<Order>>> {
    return request.get('/order/my-buy', { params })
  },

  /** 我的出售订单 */
  getMySellOrders(params: { page?: number; pageSize?: number; status?: number }): Promise<ApiResult<PageResult<Order>>> {
    return request.get('/order/my-sell', { params })
  },

  /** 取消订单 */
  cancel(id: number): Promise<ApiResult<null>> {
    return request.put(`/order/cancel/${id}`)
  },

  /** 确认收货 */
  confirm(id: number): Promise<ApiResult<null>> {
    return request.put(`/order/confirm/${id}`)
  }
}
