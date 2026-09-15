package com.campustrade.service;

import com.campustrade.vo.ChatMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket消息推送服务
 * 通过STOMP协议向用户推送实时消息
 */
@Service
@RequiredArgsConstructor
public class ChatWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送新消息给用户
     */
    public void pushNewMessage(Long userId, ChatMessageVO message) {
        // 推送到用户的个人队列
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/messages",
                message
        );
    }

    /**
     * 推送未读消息数更新
     */
    public void pushUnreadCount(Long userId, int unreadCount) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/unread",
                unreadCount
        );
    }

    /**
     * 推送新消息并更新未读数（给接收者）
     */
    public void notifyReceiver(Long receiverId, ChatMessageVO message, int unreadCount) {
        pushNewMessage(receiverId, message);
        pushUnreadCount(receiverId, unreadCount);
    }

    /**
     * 推送新消息给发送者（确认消息已送达）
     */
    public void notifySender(Long senderId, ChatMessageVO message) {
        pushNewMessage(senderId, message);
    }
}
