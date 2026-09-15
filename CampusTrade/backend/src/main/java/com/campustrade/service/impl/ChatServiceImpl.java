package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.dto.SendMessageDTO;
import com.campustrade.entity.ChatMessage;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.ChatMessageMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.ChatService;
import com.campustrade.service.ChatWebSocketService;
import com.campustrade.vo.ChatMessageVO;
import com.campustrade.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天Service实现类
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final ChatWebSocketService chatWebSocketService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(Long senderId, SendMessageDTO sendMessageDTO) {
        // 验证接收者存在
        User receiver = userMapper.selectById(sendMessageDTO.getReceiverId());
        if (receiver == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 不能给自己发消息
        if (senderId.equals(sendMessageDTO.getReceiverId())) {
            throw new BusinessException(ErrorCode.CHAT_SEND_TO_SELF);
        }

        // 至少需要消息内容或图片
        boolean hasContent = sendMessageDTO.getContent() != null && !sendMessageDTO.getContent().trim().isEmpty();
        boolean hasImage = sendMessageDTO.getImageUrl() != null && !sendMessageDTO.getImageUrl().trim().isEmpty();
        if (!hasContent && !hasImage) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容或图片不能为空");
        }

        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(sendMessageDTO.getReceiverId());
        message.setGoodsId(sendMessageDTO.getGoodsId());
        message.setContent(hasContent ? sendMessageDTO.getContent().trim() : "");
        message.setImageUrl(hasImage ? sendMessageDTO.getImageUrl().trim() : null);
        message.setIsRead(0);
        chatMessageMapper.insert(message);

        // 获取发送者信息
        User sender = userMapper.selectById(senderId);

        // 构建VO
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        vo.setSenderName(sender != null ? sender.getNickname() : "");
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : "");

        // 通过 WebSocket 推送消息给接收者
        if (sendMessageDTO.getReceiverId() != null) {
            // 获取接收者的未读数
            Integer unreadCount = chatMessageMapper.selectTotalUnreadCount(sendMessageDTO.getReceiverId());
            chatWebSocketService.notifyReceiver(sendMessageDTO.getReceiverId(), vo, unreadCount != null ? unreadCount : 0);
        }

        // 也推送给发送者（确认消息已送达）
        chatWebSocketService.notifySender(senderId, vo);

        return vo;
    }

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        List<ConversationVO> conversations = chatMessageMapper.selectConversations(userId);
        return conversations != null ? conversations : new ArrayList<>();
    }

    @Override
    public List<ChatMessageVO> getMessages(Long userId, Long otherUserId, Long goodsId) {
        // 查询消息记录
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, otherUserId)
                .or().eq(ChatMessage::getSenderId, otherUserId).eq(ChatMessage::getReceiverId, userId));
        if (goodsId != null) {
            wrapper.eq(ChatMessage::getGoodsId, goodsId);
        }
        wrapper.orderByAsc(ChatMessage::getCreateTime);
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        // 转为VO
        return messages.stream().map(msg -> {
            ChatMessageVO vo = new ChatMessageVO();
            BeanUtils.copyProperties(msg, vo);

            // 设置发送者名称和头像
            User sender = userMapper.selectById(msg.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long senderId, Long goodsId) {
        chatMessageMapper.markAsRead(userId, senderId, goodsId);
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        Integer count = chatMessageMapper.selectTotalUnreadCount(userId);
        return count != null ? count : 0;
    }
}
