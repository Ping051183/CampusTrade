import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '@/stores/user'

/**
 * WebSocket 连接管理组合式函数
 * 提供 STOMP 连接的建立、断开、订阅等功能
 */

// 全局单例连接
let globalClient: Client | null = null
let globalConnected = false
let globalDisconnected = false   // 标记是否主动断开（避免重连）

interface StompMessage {
  body: string
  headers: { [key: string]: string }
}

type MessageCallback = (body: any) => void

interface Subscription {
  destination: string
  callback: MessageCallback
  stompSub: any | null
  retryCount: number
  retryTimer: ReturnType<typeof setTimeout> | null
}

const subscriptions = new Map<string, Subscription>()

/** 重试配置 */
const RETRY_MAX_ATTEMPTS = 30           // 最多重试 30 次
const RETRY_BASE_DELAY = 500            // 初始延迟 500ms
const RETRY_MAX_DELAY = 10000           // 最大延迟 10s
const RETRY_BACKOFF_MULTIPLIER = 1.5    // 退避倍率

export function useWebSocket() {
  const connected = ref(false)
  const userStore = useUserStore()

  /**
   * 建立 WebSocket 连接
   */
  function connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (globalConnected && globalClient?.active) {
        connected.value = true
        resolve()
        return
      }

      const token = userStore.token
      if (!token) {
        reject(new Error('未登录'))
        return
      }

      globalDisconnected = false

      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.host

      // 开发环境下直连后端 WebSocket（Vite 的 WebSocket 代理在部分场景有兼容问题）
      const brokerURL = import.meta.env.DEV
        ? `ws://localhost:8080/ws`
        : `${protocol}//${host}/ws`

      const client = new Client({
        brokerURL,
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          console.log('[WS] 连接成功')
          globalConnected = true
          connected.value = true
          // 清除所有旧订阅引用（STOMP 重连后 session ID 会变，旧订阅失效）
          for (const [, sub] of subscriptions) {
            sub.stompSub = null
          }
          // 重新订阅所有待处理的订阅
          retryPendingSubscriptions()
          resolve()
        },
        onDisconnect: () => {
          console.log('[WS] 断开连接')
          globalConnected = false
          connected.value = false
        },
        onStompError: (frame) => {
          console.error('[WS] STOMP错误:', frame.headers['message'])
          connected.value = false
          reject(new Error(frame.headers['message'] || 'STOMP连接错误'))
        },
        onWebSocketClose: () => {
          connected.value = false
        }
      })

      client.activate()
      globalClient = client
    })
  }

  /**
   * 订阅用户个人队列
   */
  function subscribe(destination: string, callback: MessageCallback) {
    const key = destination

    // 如果已经订阅过，不再重复订阅
    if (subscriptions.has(key)) return

    // 如果已主动断开，不添加死订阅
    if (globalDisconnected) return

    const sub: Subscription = {
      destination,
      callback,
      stompSub: null,
      retryCount: 0,
      retryTimer: null
    }

    subscriptions.set(key, sub)

    // 如果没有活跃连接，先发起连接
    if (!globalClient?.active) {
      connect().catch(() => {})
    }

    attemptSubscribe(sub)
  }

  /**
   * 尝试订阅（带重试逻辑）
   */
  function attemptSubscribe(sub: Subscription) {
    // 如果已主动断开，不再重试
    if (globalDisconnected) return

    // 达到最大重试次数，放弃
    if (sub.retryCount >= RETRY_MAX_ATTEMPTS) {
      console.warn(`[WS] 订阅 ${sub.destination} 失败，已达最大重试次数 ${RETRY_MAX_ATTEMPTS}`)
      subscriptions.delete(sub.destination)
      return
    }

    // 如果连接已就绪，直接订阅
    if (globalClient?.active && globalClient.connected) {
      try {
        const stompSub = globalClient.subscribe(
          `/user/${userStore.userId}${sub.destination}`,
          (message: StompMessage) => {
            try {
              const body = JSON.parse(message.body)
              sub.callback(body)
            } catch (e) {
              sub.callback(message.body)
            }
          }
        )
        sub.stompSub = stompSub
        sub.retryCount = 0
        sub.retryTimer = null
        return
      } catch (e) {
        console.error(`[WS] 订阅 ${sub.destination} 失败:`, e)
        // 失败后也继续重试
      }
    }

    // 连接未就绪，安排重试（指数退避）
    const delay = Math.min(
      RETRY_BASE_DELAY * Math.pow(RETRY_BACKOFF_MULTIPLIER, sub.retryCount),
      RETRY_MAX_DELAY
    )
    sub.retryCount++

    sub.retryTimer = setTimeout(() => {
      sub.retryTimer = null
      attemptSubscribe(sub)
    }, delay)
  }

  /**
   * 连接成功后重试所有待处理的订阅
   */
  function retryPendingSubscriptions() {
    for (const [, sub] of subscriptions) {
      // 清除旧的定时器
      if (sub.retryTimer) {
        clearTimeout(sub.retryTimer)
        sub.retryTimer = null
      }
      // 如果还没有成功订阅（stompSub 为空），重新尝试
      if (!sub.stompSub) {
        sub.retryCount = 0
        attemptSubscribe(sub)
      }
    }
  }

  /**
   * 取消订阅
   */
  function unsubscribe(destination: string) {
    const sub = subscriptions.get(destination)
    if (!sub) return

    // 清除重试定时器
    if (sub.retryTimer) {
      clearTimeout(sub.retryTimer)
      sub.retryTimer = null
    }

    // 取消 STOMP 订阅
    if (sub.stompSub) {
      try {
        sub.stompSub.unsubscribe()
      } catch (e) {
        // ignore
      }
    }

    subscriptions.delete(destination)
  }

  /**
   * 主动断开连接
   */
  function disconnect() {
    globalDisconnected = true

    // 清除所有待处理的重试定时器
    for (const [, sub] of subscriptions) {
      if (sub.retryTimer) {
        clearTimeout(sub.retryTimer)
        sub.retryTimer = null
      }
      if (sub.stompSub) {
        try {
          sub.stompSub.unsubscribe()
        } catch (e) {
          // ignore
        }
      }
    }
    subscriptions.clear()

    // 断开 STOMP 连接
    if (globalClient) {
      try {
        globalClient.deactivate()
      } catch (e) {
        // ignore
      }
      globalClient = null
      globalConnected = false
      connected.value = false
    }
  }

  /**
   * 判断是否已连接
   */
  function isConnected(): boolean {
    return globalConnected && !!globalClient?.active
  }

  return {
    connected,
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    isConnected
  }
}

/**
 * 重置模块级状态（仅测试用）
 * 测试框架通过此函数清除全局状态，防止测试间泄漏
 */
export function __resetState() {
  // 清除所有待处理的重试定时器
  for (const [, sub] of subscriptions) {
    if (sub.retryTimer) {
      clearTimeout(sub.retryTimer)
      sub.retryTimer = null
    }
    sub.stompSub = null
  }
  subscriptions.clear()

  if (globalClient) {
    try { globalClient.deactivate() } catch { /* ok */ }
    globalClient = null
  }
  globalConnected = false
  globalDisconnected = false
}
