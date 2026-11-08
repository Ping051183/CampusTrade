<template>
  <header class="site-header">
    <div class="header-inner">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="28"><School /></el-icon>
        <span class="logo-text">校园二手交易平台</span>
      </div>

      <nav class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/goods" class="nav-link">商品市场</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/my/goods" class="nav-link">我的商品</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/my/orders" class="nav-link">我的订单</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/my/chats" class="nav-link msg-link">
          我的消息
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="msg-badge" />
        </router-link>
        <router-link v-if="userStore.isAdmin" to="/admin/dashboard" class="nav-link">管理后台</router-link>
      </nav>

      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <span class="notice-entry" title="系统通知" @click="$router.push('/my/notifications')">
            <el-badge :value="noticeCount" :hidden="noticeCount === 0" :max="99">
              <el-icon :size="20"><Bell /></el-icon>
            </el-badge>
          </span>
          <el-button type="primary" size="small" @click="$router.push('/goods/publish')">
            <el-icon><Plus /></el-icon>发布商品
          </el-button>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.avatar || undefined">
                {{ userStore.displayName.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.displayName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="favorites">
                  <el-icon><Star /></el-icon>我的收藏
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { chatAPI } from '@/api/chat'
import { notificationAPI } from '@/api/notification'
import { useWebSocket } from '@/composables/useWebSocket'

const router = useRouter()
const userStore = useUserStore()
const { subscribe, unsubscribe, disconnect } = useWebSocket()

const unreadCount = ref(0)
const noticeCount = ref(0)
let noticeTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchUnreadCount()
    fetchNoticeCount()
    // 每 30 秒刷新一次系统通知未读数
    noticeTimer = setInterval(fetchNoticeCount, 30000)
    // 通过 WebSocket 实时接收未读数更新
    subscribe('/queue/unread', (count: number) => {
      unreadCount.value = count
    })
  }
})

onUnmounted(() => {
  unsubscribe('/queue/unread')
  if (noticeTimer) clearInterval(noticeTimer)
})

async function fetchNoticeCount() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await notificationAPI.unreadCount()
    if (res.data !== undefined) noticeCount.value = res.data
  } catch (e) {
    // silent
  }
}

async function fetchUnreadCount() {
  try {
    const res = await chatAPI.getUnreadCount()
    if (res.data !== undefined) {
      unreadCount.value = res.data
    }
  } catch (e) {
    // silent
  }
}

function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/my/profile')
      break
    case 'favorites':
      router.push('/my/favorites')
      break
    case 'password':
      router.push('/my/password')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示').then(() => {
        disconnect()
        userStore.logout()
        router.push('/')
      }).catch(() => {})
      break
  }
}
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #409eff;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.nav-links {
  display: flex;
  gap: 8px;
}

.nav-link {
  padding: 6px 16px;
  color: #606266;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: #409eff;
  background: #ecf5ff;
}

.msg-link {
  display: flex;
  align-items: center;
  gap: 4px;
}

.msg-badge :deep(.el-badge__content) {
  background-color: #f56c6c;
  border: none;
  font-size: 11px;
  height: 16px;
  line-height: 16px;
  padding: 0 5px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice-entry {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #606266;
  padding: 4px;
  border-radius: 6px;
  transition: background 0.2s;
}

.notice-entry:hover {
  background: #f5f7fa;
  color: #409eff;
}

.notice-entry :deep(.el-badge__content) {
  background-color: #f56c6c;
  border: none;
  font-size: 11px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #303133;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
