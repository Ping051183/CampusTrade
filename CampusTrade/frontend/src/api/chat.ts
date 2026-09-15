import request from '@/utils/request'
import type { ApiResult, ChatMessageVO, ConversationVO, SendMessageDTO } from './types'

/**
 * 聊天API
 */
export const chatAPI = {
  /** 发送消息 */
  send(data: SendMessageDTO): Promise<ApiResult<ChatMessageVO>> {
    return request.post('/chat/send', data)
  },

  /** 获取会话列表 */
  getConversations(): Promise<ApiResult<ConversationVO[]>> {
    return request.get('/chat/conversations')
  },

  /** 获取聊天记录 */
  getMessages(otherUserId: number, goodsId?: number): Promise<ApiResult<ChatMessageVO[]>> {
    return request.get('/chat/messages', { params: { otherUserId, goodsId } })
  },

  /** 标记消息为已读 */
  markAsRead(senderId: number, goodsId?: number): Promise<ApiResult<null>> {
    return request.put('/chat/read', null, { params: { senderId, goodsId } })
  },

  /** 获取未读消息数 */
  getUnreadCount(): Promise<ApiResult<number>> {
    return request.get('/chat/unread/count')
  },

  /** 上传聊天图片 */
  uploadImage(file: File): Promise<ApiResult<string>> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/chat/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    })
  }
}
