<template>
  <div class="chats-page">
    <Header />
    <div class="page-container">
      <h2 class="section-title">我的消息</h2>

      <div v-if="loading" class="empty-state">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="conversations.length === 0" class="empty-state">
        <el-icon :size="60"><ChatDotSquare /></el-icon>
        <p>暂无消息</p>
        <p class="sub-text">在商品详情页点击"联系卖家"即可开始聊天</p>
      </div>

      <div v-else class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="`${conv.otherUserId}-${conv.goodsId}`"
          class="conversation-card"
          @click="openChat(conv)"
        >
          <div class="conv-left">
            <el-badge :value="conv.unreadCount" :hidden="conv.unreadCount === 0" class="avatar-badge">
              <el-avatar :size="50" :src="conv.otherUserAvatar || undefined">
                {{ conv.otherUserName?.charAt(0)?.toUpperCase() || '?' }}
              </el-avatar>
            </el-badge>
          </div>
          <div class="conv-center">
            <div class="conv-top">
              <span class="conv-name">{{ conv.otherUserName }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-goods" v-if="conv.goodsTitle">
              <span class="goods-tag">商品:</span>
              <span class="goods-title">{{ conv.goodsTitle }}</span>
              <span class="goods-price" v-if="conv.goodsPrice">¥{{ conv.goodsPrice }}</span>
            </div>
            <div class="conv-last-msg" :class="{ 'is-image': !!conv.lastMessageImageUrl }">
              <template v-if="conv.lastMessageImageUrl">
                <el-image
                  :src="conv.lastMessageImageUrl"
                  fit="cover"
                  class="last-msg-thumb"
                  :preview-src-list="[conv.lastMessageImageUrl]"
                  preview-teleported
                />
                <span class="thumb-label">[图片]</span>
              </template>
              <template v-else>
                {{ conv.lastMessage || '暂无消息' }}
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotSquare } from '@element-plus/icons-vue'
import { chatAPI } from '@/api/chat'
import { useWebSocket } from '@/composables/useWebSocket'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { ConversationVO, ChatMessageVO } from '@/api/types'

const router = useRouter()
const { subscribe, unsubscribe } = useWebSocket()

const conversations = ref<ConversationVO[]>([])
const loading = ref(true)

onMounted(() => {
  loadConversations()
  // 收到新消息时刷新会话列表
  subscribe('/queue/messages', handleNewMessageNotification)
})

onUnmounted(() => {
  unsubscribe('/queue/messages')
})

/**
 * 收到 WebSocket 新消息时刷新会话列表
 */
function handleNewMessageNotification(msg: ChatMessageVO) {
  // 延迟一下让数据库完成更新
  setTimeout(loadConversations, 300)
}

async function loadConversations() {
  try {
    const res = await chatAPI.getConversations()
    if (res.data) {
      conversations.value = res.data
    }
  } catch (e) {
    console.error('加载会话列表失败', e)
  } finally {
    loading.value = false
  }
}

function openChat(conv: ConversationVO) {
  router.push({
    path: '/chat',
    query: {
      userId: conv.otherUserId,
      userName: conv.otherUserName,
      goodsId: conv.goodsId,
      goodsTitle: conv.goodsTitle
    }
  })
}

function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`

  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  return `${month}-${day}`
}
</script>

<style scoped>
.chats-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.conversation-list {
  max-width: 700px;
  margin: 0 auto;
  width: 100%;
}

.conversation-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.conversation-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.avatar-badge :deep(.el-badge__content) {
  background-color: #f56c6c;
}

.conv-center {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.conv-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.conv-goods {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-flex;
  max-width: 100%;
}

.goods-tag {
  color: #909399;
  flex-shrink: 0;
}

.goods-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
}

.goods-price {
  color: #f56c6c;
  font-weight: 600;
  flex-shrink: 0;
}

.conv-last-msg {
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.conv-last-msg.is-image {
  white-space: normal;
}

.last-msg-thumb {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #f0f2f5;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.last-msg-thumb:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.thumb-label {
  font-size: 12px;
  color: #c0c4cc;
  flex-shrink: 0;
}

.sub-text {
  font-size: 13px;
  color: #c0c4cc;
  margin-top: 8px;
}
</style>
