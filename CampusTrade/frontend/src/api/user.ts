import request from '@/utils/request'
import type { ApiResult, LoginVO, UserVO, LoginDTO, RegisterDTO, UserUpdateDTO, UpdatePasswordDTO } from './types'

/**
 * 用户API
 */
export const userAPI = {
  /** 登录 */
  login(data: LoginDTO): Promise<ApiResult<LoginVO>> {
    return request.post('/auth/login', data)
  },

  /** 注册 */
  register(data: RegisterDTO): Promise<ApiResult<null>> {
    return request.post('/auth/register', data)
  },

  /** 获取用户信息 */
  getUserInfo(): Promise<ApiResult<UserVO>> {
    return request.get('/user/info')
  },

  /** 修改个人信息 */
  updateUserInfo(data: UserUpdateDTO): Promise<ApiResult<null>> {
    return request.put('/user/update', data)
  },

  /** 修改密码 */
  updatePassword(data: UpdatePasswordDTO): Promise<ApiResult<null>> {
    return request.put('/user/password', data)
  },

  /** 上传头像 */
  uploadAvatar(file: File): Promise<ApiResult<string>> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /** 更新头像URL */
  updateAvatar(avatarUrl: string): Promise<ApiResult<string>> {
    return request.put('/user/avatar', { avatarUrl })
  }
}
