<template>
  <div class="chat-detail-page">
    <Header />

    <div class="chat-container">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <el-button text @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div class="chat-user-info">
          <el-avatar :size="36" :src="otherUserAvatar || undefined">
            {{ otherUserName?.charAt(0)?.toUpperCase() || '?' }}
          </el-avatar>
          <span class="chat-user-name">{{ otherUserName }}</span>
          <el-tag v-if="!wsConnected" size="small" type="warning" effect="plain">重连中...</el-tag>
        </div>
        <div class="chat-goods-info" v-if="goodsTitle">
          <el-tag size="small" type="info" closable @close="goodsId = 0; goodsTitle = ''">
            {{ goodsTitle }}
          </el-tag>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef" @scroll="onScroll">
        <div v-if="messages.length === 0" class="empty-msg">
          <el-icon :size="48"><ChatLineSquare /></el-icon>
          <p>开始聊天吧</p>
        </div>
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'is-self': msg.senderId === currentUserId }"
        >
          <el-avatar
            :size="36"
            :src="msg.senderAvatar || undefined"
            class="msg-avatar"
          >
            {{ msg.senderName?.charAt(0)?.toUpperCase() || '?' }}
          </el-avatar>
          <div class="msg-body">
            <div v-if="msg.imageUrl" class="msg-image-wrapper">
              <el-image
                :src="msg.imageUrl"
                fit="contain"
                class="msg-image"
                :preview-src-list="[msg.imageUrl || '']"
                preview-teleported
              />
            </div>
            <div v-if="msg.content" class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ msg.createTime }}</div>
          </div>
        </div>
      </div>

      <!-- 图片预览（发送前） -->
      <transition name="slide-up">
        <div v-if="previewImageUrl" class="image-preview-bar">
          <div class="preview-content">
            <el-image :src="previewImageUrl" fit="contain" class="preview-img" />
            <div class="preview-info">
              <span class="preview-label">图片预览</span>
              <el-button size="small" type="danger" text @click="clearPreview">
                <el-icon><Close /></el-icon> 取消
              </el-button>
            </div>
          </div>
        </div>
      </transition>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="input-toolbar">
          <el-upload
            ref="uploadRef"
            :show-file-list="false"
            :auto-upload="false"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :on-change="handleFileSelect"
          >
            <el-button text class="toolbar-btn" title="发送图片">
              <el-icon :size="22"><Picture /></el-icon>
            </el-button>
          </el-upload>
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            maxlength="1000"
            show-word-limit
            @keydown.ctrl.enter="sendMessage"
            :disabled="sending"
            class="msg-input"
          />
        </div>
        <div class="input-actions">
          <span class="input-hint">按 Ctrl+Enter 发送</span>
          <div class="input-buttons">
            <el-button
              type="primary"
              :loading="sending"
              @click="sendMessage"
              :disabled="!canSend"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatLineSquare, Picture, Close } from '@element-plus/icons-vue'
import type { UploadInstance } from 'element-plus'
import { chatAPI } from '@/api/chat'
import { useUserStore } from '@/stores/user'
import { useWebSocket } from '@/composables/useWebSocket'
import { compressImage } from '@/utils/imageCompress'
import Header from '@/components/Header.vue'
import type { ChatMessageVO } from '@/api/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { connected: wsConnected, subscribe, unsubscribe } = useWebSocket()

const messages = ref<ChatMessageVO[]>([])
const inputContent = ref('')
const sending = ref(false)
const uploading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const uploadRef = ref<UploadInstance>()
const currentUserId = computed(() => userStore.userId)
const otherUserId = ref(Number(route.query.userId) || 0)
const otherUserName = ref((route.query.userName as string) || '')
const otherUserAvatar = ref('')
const goodsId = ref(Number(route.query.goodsId) || 0)
const goodsTitle = ref((route.query.goodsTitle as string) || '')

const selectedFile = ref<File | null>(null)
const previewImageUrl = ref('')

const canSend = computed(() => {
  return (inputContent.value.trim() || selectedFile.value) && !sending.value && !uploading.value
})

let isAtBottom = true

onMounted(() => {
  loadMessages()
  // 订阅 WebSocket 实时消息
  subscribe('/queue/messages', handleNewMessage)
})

onUnmounted(() => {
  unsubscribe('/queue/messages')
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
})

/**
 * 处理 WebSocket 推送的新消息
 */
function handleNewMessage(msg: ChatMessageVO) {
  // 只处理属于当前聊天的消息
  const isRelevant =
    (msg.senderId === otherUserId.value && msg.receiverId === currentUserId.value) ||
    (msg.senderId === currentUserId.value && msg.receiverId === otherUserId.value)

  if (!isRelevant) return

  // 检查是否已存在（避免重复）
  const exists = messages.value.some(m => m.id === msg.id)
  if (exists) return

  messages.value.push(msg)

  // 标记为已读
  if (msg.senderId === otherUserId.value) {
    chatAPI.markAsRead(otherUserId.value, goodsId.value || undefined)
  }

  nextTick(() => {
    if (isAtBottom) scrollToBottom()
  })
}

async function loadMessages() {
  try {
    const res = await chatAPI.getMessages(otherUserId.value, goodsId.value || undefined)
    if (res.data) {
      messages.value = res.data
      await chatAPI.markAsRead(otherUserId.value, goodsId.value || undefined)
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

function handleFileSelect(uploadFile: any) {
  const file = uploadFile.raw
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }

  selectedFile.value = file
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = URL.createObjectURL(file)
}

function clearPreview() {
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = ''
  selectedFile.value = null
}

async function sendMessage() {
  const content = inputContent.value.trim()
  const file = selectedFile.value

  if ((!content && !file) || !otherUserId.value || sending.value || uploading.value) return

  sending.value = true

  try {
    let imageUrl: string | undefined

    if (file) {
      uploading.value = true
      // 先压缩图片再上传
      const compressedFile = await compressImage(file)
      const uploadRes = await chatAPI.uploadImage(compressedFile)
      if (uploadRes.data) {
        imageUrl = uploadRes.data
      }
      uploading.value = false
    }

    // 发送消息（通过 REST API）
    const res = await chatAPI.send({
      receiverId: otherUserId.value,
      goodsId: goodsId.value || undefined,
      content: content || undefined,
      imageUrl
    })

    if (res.data) {
      // 消息会通过 WebSocket 推送回来，所以不需要手动添加
      // 但为了更快的 UI 响应，可以直接添加（WebSocket 会负责去重）
      const exists = messages.value.some(m => m.id === res.data!.id)
      if (!exists) {
        messages.value.push(res.data)
      }
      inputContent.value = ''
      clearPreview()
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    // Error handled in interceptor
  } finally {
    sending.value = false
    uploading.value = false
  }
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    isAtBottom = true
  }
}

function onScroll() {
  if (!messageListRef.value) return
  const el = messageListRef.value
  const threshold = 50
  isAtBottom = el.scrollHeight - el.scrollTop - el.clientHeight < threshold
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.chat-detail-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 700px;
  margin: 0 auto;
  width: 100%;
  padding: 0 20px 20px;
  background: #fff;
  border-radius: 12px 12px 0 0;
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.back-btn {
  flex-shrink: 0;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-user-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.chat-goods-info {
  margin-left: auto;
}

/* ===== 消息列表 ===== */

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.empty-msg {
  text-align: center;
  padding: 60px 20px;
  color: #c0c4cc;
}

.empty-msg p {
  margin-top: 12px;
  font-size: 14px;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 80%;
}

.message-item.is-self {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  margin-top: 4px;
}

.msg-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.is-self .msg-body {
  align-items: flex-end;
}

.msg-content {
  background: #f0f2f5;
  padding: 10px 14px;
  border-radius: 16px 16px 16px 4px;
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  word-break: break-word;
  white-space: pre-wrap;
}

.is-self .msg-content {
  background: #409eff;
  color: #fff;
  border-radius: 16px 16px 4px 16px;
}

.msg-image-wrapper {
  max-width: 260px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  transition: box-shadow 0.2s;
}

.msg-image-wrapper:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
}

.msg-image {
  display: block;
  width: 100%;
  max-height: 260px;
  object-fit: contain;
  background: #fafafa;
}

.is-self .msg-image-wrapper {
  border-color: #409eff;
}

.msg-time {
  font-size: 11px;
  color: #c0c4cc;
  padding: 0 4px;
}

/* ===== 图片预览条 ===== */

.image-preview-bar {
  flex-shrink: 0;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
  padding: 8px 12px;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

.preview-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-img {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: #fff;
  object-fit: cover;
  border: 1px solid #dcdfe6;
}

.preview-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-label {
  font-size: 13px;
  color: #909399;
}

/* ===== 输入区域 ===== */

.chat-input-area {
  flex-shrink: 0;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.input-toolbar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.toolbar-btn {
  flex-shrink: 0;
  padding: 6px;
  color: #909399;
  transition: color 0.2s;
}

.toolbar-btn:hover {
  color: #409eff;
}

.msg-input {
  flex: 1;
}

.msg-input :deep(.el-textarea__inner) {
  min-height: 44px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: #c0c4cc;
}

.input-buttons {
  display: flex;
  gap: 8px;
}
</style>
