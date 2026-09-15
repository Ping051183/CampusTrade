import request from '@/utils/request'
import type { ApiResult, PageResult, UserVO, Goods, Report, Notice } from './types'

/**
 * 管理员API
 */
export const adminAPI = {
  /** 数据统计 */
  getDashboard(): Promise<ApiResult<Record<string, any>>> {
    return request.get('/admin/dashboard')
  },

  /** 用户列表 */
  getUserList(params: {
    page?: number
    pageSize?: number
    keyword?: string
  }): Promise<ApiResult<PageResult<UserVO>>> {
    return request.get('/admin/user/list', { params })
  },

  /** 修改用户状态 */
  updateUserStatus(id: number, status: number): Promise<ApiResult<null>> {
    return request.put(`/admin/user/status/${id}`, null, { params: { status } })
  },

  /** 商品列表（管理员） */
  getGoodsList(params: {
    page?: number
    pageSize?: number
    status?: number
  }): Promise<ApiResult<PageResult<Goods>>> {
    return request.get('/admin/goods/list', { params })
  },

  /** 商品审核 */
  auditGoods(id: number, status: number, rejectReason?: string): Promise<ApiResult<null>> {
    return request.put(`/admin/goods/audit/${id}`, null, { params: { status, rejectReason } })
  },

  /** 删除商品（管理员） */
  deleteGoods(id: number): Promise<ApiResult<null>> {
    return request.delete(`/admin/goods/delete/${id}`)
  },

  /** 举报列表 */
  getReportList(params: {
    page?: number
    pageSize?: number
    status?: number
  }): Promise<ApiResult<PageResult<Report>>> {
    return request.get('/admin/report/list', { params })
  },

  /** 处理举报 */
  handleReport(id: number, status: number, result: string): Promise<ApiResult<null>> {
    return request.put(`/admin/report/handle/${id}`, null, { params: { status, result } })
  },

  /** 发布公告 */
  addNotice(data: Notice): Promise<ApiResult<null>> {
    return request.post('/admin/notice/add', data)
  },

  /** 删除公告 */
  deleteNotice(id: number): Promise<ApiResult<null>> {
    return request.delete(`/admin/notice/delete/${id}`)
  }
}
