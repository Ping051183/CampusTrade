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

  /** 支付订单（模拟支付） */
  pay(id: number): Promise<ApiResult<null>> {
    return request.put(`/order/pay/${id}`)
  },

  /** 卖家发货 */
  ship(id: number): Promise<ApiResult<null>> {
    return request.put(`/order/ship/${id}`)
  },

  /** 确认收货 */
  confirm(id: number): Promise<ApiResult<null>> {
    return request.put(`/order/confirm/${id}`)
  },

  /** 买家申请退款 */
  applyRefund(id: number, reason?: string): Promise<ApiResult<null>> {
    return request.post(`/order/refund/${id}`, null, { params: { reason } })
  },

  /** 卖家同意退款 */
  agreeRefund(id: number): Promise<ApiResult<null>> {
    return request.put(`/order/refund/agree/${id}`)
  },

  /** 卖家拒绝退款 */
  rejectRefund(id: number): Promise<ApiResult<null>> {
    return request.put(`/order/refund/reject/${id}`)
  }
}
