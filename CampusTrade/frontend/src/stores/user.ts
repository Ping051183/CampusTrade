import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userAPI } from '@/api/user'
import type { LoginVO } from '@/api/types'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string | null>(null)
  const userId = ref<number | null>(null)
  const username = ref<string>('')
  const nickname = ref<string>('')
  const avatar = ref<string>('')
  const role = ref<number>(1)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 0)
  const displayName = computed(() => nickname.value || username.value)

  /**
   * 解析 JWT 的过期时间（毫秒），解析失败返回 null
   */
  function parseTokenExp(token: string): number | null {
    try {
      const payload = token.split('.')[1]
      if (!payload) return null
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
      const json = JSON.parse(atob(normalized))
      return typeof json.exp === 'number' ? json.exp * 1000 : null
    } catch (e) {
      return null
    }
  }

  /**
   * 从本地存储恢复登录状态
   */
  function initFromStorage() {
    const saved = localStorage.getItem('userInfo')
    if (saved) {
      try {
        const data = JSON.parse(saved)
        // 没有 token 或 token 已过期：视为未登录，避免用无效凭证反复请求
        const exp = data.token ? parseTokenExp(data.token) : null
        if (!data.token || (exp !== null && exp <= Date.now())) {
          clearUser()
          return
        }
        token.value = data.token
        userId.value = data.userId
        username.value = data.username
        nickname.value = data.nickname
        avatar.value = data.avatar
        role.value = data.role
      } catch (e) {
        clearUser()
      }
    }
  }

  /**
   * 保存登录状态到本地存储
   */
  function saveToStorage(data: LoginVO) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    avatar.value = data.avatar
    role.value = data.role
    localStorage.setItem('userInfo', JSON.stringify(data))
  }

  /**
   * 清除登录状态
   */
  function clearUser() {
    token.value = null
    userId.value = null
    username.value = ''
    nickname.value = ''
    avatar.value = ''
    role.value = 1
    localStorage.removeItem('userInfo')
  }

  /**
   * 登录
   */
  async function login(loginData: { username: string; password: string }) {
    const res = await userAPI.login(loginData)
    if (res.code === 200 && res.data) {
      saveToStorage(res.data)
      return true
    }
    return false
  }

  /**
   * 注册
   */
  async function register(registerData: any) {
    const res = await userAPI.register(registerData)
    return res.code === 200
  }

  /**
   * 注销
   */
  function logout() {
    clearUser()
  }

  /**
   * 更新用户信息
   */
  async function fetchUserInfo() {
    if (!userId.value) return
    const res = await userAPI.getUserInfo()
    if (res.code === 200 && res.data) {
      nickname.value = res.data.nickname
      avatar.value = res.data.avatar
    }
  }

  return {
    token, userId, username, nickname, avatar, role,
    isLoggedIn, isAdmin, displayName,
    initFromStorage, login, register, logout, fetchUserInfo
  }
})
