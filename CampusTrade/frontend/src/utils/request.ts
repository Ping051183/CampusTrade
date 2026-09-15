import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

/** 统一响应格式 */
interface ApiResult<T = any> {
  code: number
  message: string
  data?: T
}

/** 创建Axios实例 */
const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/** 请求拦截器 - 添加Token */
request.interceptors.request.use(
  (config) => {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      try {
        const { token } = JSON.parse(userInfo)
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
      } catch (e) {
        // ignore
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器 - 统一处理错误 */
request.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const { data } = response
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('权限不足')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
