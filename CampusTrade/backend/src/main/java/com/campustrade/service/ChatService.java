package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.dto.SendMessageDTO;
import com.campustrade.entity.ChatMessage;
import com.campustrade.vo.ChatMessageVO;
import com.campustrade.vo.ConversationVO;

import java.util.List;

/**
 * 聊天Service接口
 */
public interface ChatService extends IService<ChatMessage> {

    /**
     * 发送消息
     */
    ChatMessageVO sendMessage(Long senderId, SendMessageDTO sendMessageDTO);

    /**
     * 获取会话列表
     */
    List<ConversationVO> getConversations(Long userId);

    /**
     * 获取聊天记录
     */
    List<ChatMessageVO> getMessages(Long userId, Long otherUserId, Long goodsId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long userId, Long senderId, Long goodsId);

    /**
     * 获取未读消息总数
     */
    Integer getUnreadCount(Long userId);
}
