package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.cache.CacheService;
import com.campustrade.dto.SendMessageDTO;
import com.campustrade.entity.ChatMessage;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.ChatMessageMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.ChatWebSocketService;
import com.campustrade.vo.ChatMessageVO;
import com.campustrade.vo.ConversationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 单元测试")
class ChatServiceImplTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ChatWebSocketService chatWebSocketService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Captor
    private ArgumentCaptor<ChatMessage> messageCaptor;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");
        sender.setNickname("发送者");

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");
        receiver.setNickname("接收者");
    }

    @Nested
    @DisplayName("发送消息")
    class SendMessageTests {

        @Test
        @DisplayName("发送文本消息成功")
        void sendMessage_textSuccess() {
            SendMessageDTO dto = new SendMessageDTO();
            dto.setReceiverId(2L);
            dto.setContent("Hello, this is a test message");
            dto.setGoodsId(100L);

            when(userMapper.selectById(2L)).thenReturn(receiver);
            when(userMapper.selectById(1L)).thenReturn(sender);
            when(chatMessageMapper.selectTotalUnreadCount(2L)).thenReturn(5);
            doNothing().when(chatWebSocketService).notifyReceiver(anyLong(), any(), anyInt());
            doNothing().when(chatWebSocketService).notifySender(anyLong(), any());

            ChatMessageVO result = chatService.sendMessage(1L, dto);

            assertNotNull(result);
            assertEquals("Hello, this is a test message", result.getContent());
            assertEquals("发送者", result.getSenderName());
            assertEquals(0, result.getIsRead());

            verify(chatMessageMapper).insert(messageCaptor.capture());
            ChatMessage saved = messageCaptor.getValue();
            assertEquals(1L, saved.getSenderId());
            assertEquals(2L, saved.getReceiverId());
            assertEquals(100L, saved.getGoodsId());
            assertEquals("Hello, this is a test message", saved.getContent());
            assertEquals(0, saved.getIsRead());
        }

        @Test
        @DisplayName("发送图片消息成功（内容为空时使用'[图片]'占位）")
        void sendMessage_imageSuccess() {
            SendMessageDTO dto = new SendMessageDTO();
            dto.setReceiverId(2L);
            dto.setImageUrl("/uploads/chat/image.jpg");
            dto.setGoodsId(100L);

            when(userMapper.selectById(2L)).thenReturn(receiver);
            when(userMapper.selectById(1L)).thenReturn(sender);
            when(chatMessageMapper.selectTotalUnreadCount(2L)).thenReturn(3);
            doNothing().when(chatWebSocketService).notifyReceiver(anyLong(), any(), anyInt());
            doNothing().when(chatWebSocketService).notifySender(anyLong(), any());

            ChatMessageVO result = chatService.sendMessage(1L, dto);

            assertNotNull(result);
            assertEquals("/uploads/chat/image.jpg", result.getImageUrl());

            verify(chatMessageMapper).insert(messageCaptor.capture());
            assertEquals("/uploads/chat/image.jpg", messageCaptor.getValue().getImageUrl());
            assertEquals("", messageCaptor.getValue().getContent());
        }

        @Test
        @DisplayName("接收者不存在时抛出异常")
        void sendMessage_receiverNotFound() {
            SendMessageDTO dto = new SendMessageDTO();
            dto.setReceiverId(999L);
            dto.setContent("Hello");

            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.sendMessage(1L, dto));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("不能给自己发消息")
        void sendMessage_toSelf() {
            SendMessageDTO dto = new SendMessageDTO();
            dto.setReceiverId(1L);
            dto.setContent("Hello to self");

            when(userMapper.selectById(1L)).thenReturn(sender);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.sendMessage(1L, dto));
            assertEquals(ErrorCode.CHAT_SEND_TO_SELF.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("消息内容为空且无图片时抛出异常")
        void sendMessage_noContentNoImage() {
            SendMessageDTO dto = new SendMessageDTO();
            dto.setReceiverId(2L);

            when(userMapper.selectById(2L)).thenReturn(receiver);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.sendMessage(1L, dto));
            assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("获取会话列表")
    class GetConversationsTests {

        @Test
        @DisplayName("有会话时返回列表")
        void getConversations_withData() {
            ConversationVO conv1 = new ConversationVO();
            conv1.setOtherUserId(2L);
            conv1.setLastMessage("Hello");
            conv1.setUnreadCount(3);

            ConversationVO conv2 = new ConversationVO();
            conv2.setOtherUserId(3L);
            conv2.setLastMessage("Hi there");
            conv2.setUnreadCount(0);

            List<ConversationVO> mockList = List.of(conv1, conv2);
            when(chatMessageMapper.selectConversations(1L)).thenReturn(mockList);

            List<ConversationVO> result = chatService.getConversations(1L);

            assertEquals(2, result.size());
            assertEquals("Hello", result.get(0).getLastMessage());
            assertEquals(3, result.get(0).getUnreadCount());
        }

        @Test
        @DisplayName("无会话时返回空列表")
        void getConversations_empty() {
            when(chatMessageMapper.selectConversations(1L)).thenReturn(null);

            List<ConversationVO> result = chatService.getConversations(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("获取聊天记录")
    class GetMessagesTests {

        @Test
        @DisplayName("返回聊天记录并按时间排序")
        void getMessages_success() {
            ChatMessage msg1 = new ChatMessage();
            msg1.setId(1L);
            msg1.setSenderId(1L);
            msg1.setReceiverId(2L);
            msg1.setContent("Message 1");
            msg1.setCreateTime(LocalDateTime.now().minusMinutes(5));

            ChatMessage msg2 = new ChatMessage();
            msg2.setId(2L);
            msg2.setSenderId(2L);
            msg2.setReceiverId(1L);
            msg2.setContent("Message 2");
            msg2.setCreateTime(LocalDateTime.now().minusMinutes(3));

            List<ChatMessage> messages = List.of(msg1, msg2);
            when(chatMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(messages);
            when(userMapper.selectById(1L)).thenReturn(sender);
            when(userMapper.selectById(2L)).thenReturn(receiver);

            List<ChatMessageVO> result = chatService.getMessages(1L, 2L, null);

            assertEquals(2, result.size());
            assertEquals("Message 1", result.get(0).getContent());
        }

        @Test
        @DisplayName("无记录时返回空列表")
        void getMessages_empty() {
            when(chatMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            List<ChatMessageVO> result = chatService.getMessages(1L, 2L, null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("标记消息为已读")
    void markAsRead() {
        chatService.markAsRead(1L, 2L, 100L);

        verify(chatMessageMapper).markAsRead(1L, 2L, 100L);
    }

    @Nested
    @DisplayName("获取未读消息数")
    class GetUnreadCountTests {

        @Test
        @DisplayName("有未读消息时返回数量")
        void getUnreadCount_withData() {
            when(chatMessageMapper.selectTotalUnreadCount(1L)).thenReturn(5);

            Integer count = chatService.getUnreadCount(1L);

            assertEquals(5, count);
        }

        @Test
        @DisplayName("无未读消息时返回 0")
        void getUnreadCount_null() {
            when(chatMessageMapper.selectTotalUnreadCount(1L)).thenReturn(null);

            Integer count = chatService.getUnreadCount(1L);

            assertEquals(0, count);
        }
    }
}
