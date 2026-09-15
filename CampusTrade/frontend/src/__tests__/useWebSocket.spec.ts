import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useWebSocket, __resetState } from '@/composables/useWebSocket'

// ============================================================
// Mock @stomp/stompjs
// ============================================================
const { mockActivate, mockDeactivate, mockSubscribe, triggerOnConnect, MockClientCtor } = vi.hoisted(() => {
  let onConnectCb: (() => void) | null = null

  const mockActivate = vi.fn(() => {
    setTimeout(() => { if (onConnectCb) onConnectCb?.() }, 0)
  })
  const mockDeactivate = vi.fn()
  const mockSubscribe = vi.fn()

  function MockClientCtor(config: any) {
    onConnectCb = config.onConnect || null
    this.activate = mockActivate
    this.deactivate = mockDeactivate
    this.subscribe = mockSubscribe
    this.connected = true
    this.active = true
  }

  return {
    mockActivate,
    mockDeactivate,
    mockSubscribe,
    triggerOnConnect: () => onConnectCb?.(),
    MockClientCtor,
  }
})

vi.mock('@stomp/stompjs', () => ({ Client: MockClientCtor }))

function makeStorage(): Storage {
  let s: Record<string, string> = {}
  return {
    getItem: (k: string) => s[k] ?? null,
    setItem: (k: string, v: string) => { s[k] = v },
    removeItem: (k: string) => { delete s[k] },
    clear: () => { s = {} },
    get length() { return Object.keys(s).length },
    key: (i: number) => Object.keys(s)[i] ?? null,
  }
}

function loginUser() {
  const storage = makeStorage()
  storage.setItem('userInfo', JSON.stringify({
    token: 'test-token', userId: 1, username: 'testuser',
    nickname: '测试', avatar: '', role: 1,
  }))
  Object.defineProperty(window, 'localStorage', { value: storage, configurable: true })
  useUserStore().initFromStorage()
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  // 每次调用返回新对象，使重连测试能验证 stompSub 引用是否变化
  mockSubscribe.mockImplementation(() => ({
    id: 'sub-' + Date.now() + Math.random(),
    unsubscribe: vi.fn(),
  }))
  vi.useFakeTimers()
  Object.defineProperty(window, 'localStorage', { value: makeStorage(), configurable: true })
})

afterEach(() => {
  vi.useRealTimers()
  __resetState()
})

// ======================== TESTS ========================

describe('useWebSocket', () => {

  describe('connect()', () => {

    it('拒绝无 token', async () => {
      const w = useWebSocket()
      await expect(w.connect()).rejects.toThrow('未登录')
      expect(mockActivate).not.toHaveBeenCalled()
    })

    it('有 token 成功连接', async () => {
      loginUser()
      const w = useWebSocket()
      const p = w.connect()
      // connect() 内 new Client() + client.activate() 同步执行
      expect(mockActivate).toHaveBeenCalledTimes(1)
      await vi.runAllTimersAsync()
      await expect(p).resolves.toBeUndefined()
      expect(w.isConnected()).toBe(true)
    })
  })

  describe('subscribe() 场景', () => {

    it('触发 connect 并调 subscribe', () => {
      loginUser()
      const w = useWebSocket()
      // subscribe 内部同步调用 connect() + attemptSubscribe()
      w.subscribe('/queue/messages', vi.fn())
      // mockActivate 应该在 connect() 内 client.activate() 时被调用
      expect(mockActivate).toHaveBeenCalledTimes(1)
      // mockSubscribe 应该在 attemptSubscribe 内 globalClient.subscribe() 时被调用
      expect(mockSubscribe).toHaveBeenCalledWith('/user/1/queue/messages', expect.any(Function))
    })

    it('重复 destination 去重', () => {
      loginUser()
      const w = useWebSocket()
      w.subscribe('/queue/messages', vi.fn())
      w.subscribe('/queue/messages', vi.fn())
      expect(mockActivate).toHaveBeenCalledTimes(1)
    })

    it('断开后不添加', () => {
      const w = useWebSocket()
      w.disconnect()
      w.subscribe('/queue/messages', vi.fn())
      expect(mockActivate).not.toHaveBeenCalled()
    })
  })

  describe('disconnect()', () => {
    it('断开后 isConnected 为 false', async () => {
      loginUser()
      const w = useWebSocket()
      w.subscribe('/queue/messages', vi.fn())
      await vi.runAllTimersAsync()
      expect(w.isConnected()).toBe(true)
      w.disconnect()
      expect(w.isConnected()).toBe(false)
      expect(mockDeactivate).toHaveBeenCalled()
    })
  })

  describe('unsubscribe()', () => {
    it('取消 STOMP 订阅', async () => {
      loginUser()
      const w = useWebSocket()
      w.subscribe('/queue/messages', vi.fn())
      // 在 onConnect 触发前取消订阅 — 此时 stompSub 还是首次订阅的返回值
      const sub = mockSubscribe.mock.results[0]?.value
      w.unsubscribe('/queue/messages')
      expect(sub).toBeDefined()
      expect(sub.unsubscribe).toHaveBeenCalled()
    })
  })

  describe('重试', () => {
    it('30 次重试后放弃', async () => {
      const w = useWebSocket()
      w.subscribe('/queue/messages', vi.fn())
      for (let i = 0; i < 400; i++) {
        await vi.advanceTimersByTimeAsync(1000)
      }
      expect(() => w.subscribe('/queue/other', vi.fn())).not.toThrow()
    })

    it('重连后重新订阅', async () => {
      loginUser()
      const w = useWebSocket()
      w.subscribe('/queue/messages', vi.fn())
      await vi.runAllTimersAsync()
      // 至此: 1次初始订阅 + onConnect回调触发的1次重试 = 2
      expect(mockSubscribe).toHaveBeenCalledTimes(2)
      triggerOnConnect()
      await vi.runAllTimersAsync()
      // triggerOnConnect 触发 onConnect → 重新订阅 = 第3次
      expect(mockSubscribe).toHaveBeenCalledTimes(3)
    })

    it('重连时旧订阅对象与新订阅不同', async () => {
      loginUser()
      const w = useWebSocket()
      w.subscribe('/queue/messages', vi.fn())
      await vi.runAllTimersAsync()
      const r1 = mockSubscribe.mock.results[0]?.value
      triggerOnConnect()
      await vi.runAllTimersAsync()
      const r2 = mockSubscribe.mock.results[1]?.value
      expect(r1).toBeDefined()
      expect(r2).toBeDefined()
      expect(r1).not.toBe(r2)
    })
  })

  describe('消息解析', () => {
    it('JSON 解析', async () => {
      loginUser()
      const cb = vi.fn()
      const w = useWebSocket()
      w.subscribe('/queue/messages', cb)
      await vi.runAllTimersAsync()
      const handler = mockSubscribe.mock.calls[0]?.[1]
      handler?.({ body: JSON.stringify({ a: 1 }), headers: {} })
      expect(cb).toHaveBeenCalledWith({ a: 1 })
    })

    it('纯文本', async () => {
      loginUser()
      const cb = vi.fn()
      const w = useWebSocket()
      w.subscribe('/queue/messages', cb)
      await vi.runAllTimersAsync()
      const handler = mockSubscribe.mock.calls[0]?.[1]
      handler?.({ body: 'hello', headers: {} })
      expect(cb).toHaveBeenCalledWith('hello')
    })
  })
})
