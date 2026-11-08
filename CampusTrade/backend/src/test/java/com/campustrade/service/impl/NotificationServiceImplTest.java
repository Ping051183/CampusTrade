package com.campustrade.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.PageResult;
import com.campustrade.entity.Notification;
import com.campustrade.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationServiceImpl 单元测试：落库字段、分页/未读筛选、已读标记。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 单元测试")
class NotificationServiceImplTest {

    private static final Long USER_ID = 3L;
    private static final Long RELATED_ID = 8L;

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationMapper);
    }

    @Test
    @DisplayName("发送通知：未读状态为 0，业务字段原样落库")
    void send_fillsAllFields() {
        notificationService.send(USER_ID, "ORDER_PAID", "订单已付款", "买家已付款", RELATED_ID);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("ORDER_PAID", saved.getType());
        assertEquals("订单已付款", saved.getTitle());
        assertEquals("买家已付款", saved.getContent());
        assertEquals(RELATED_ID, saved.getRelatedId());
        assertEquals(0, saved.getIsRead().intValue());
    }

    @Test
    @DisplayName("分页查询返回 PageResult 结构")
    void page_returnsPageResult() {
        Page<Notification> page = new Page<>(2, 5);
        page.setRecords(List.of(new Notification()));
        page.setTotal(7);
        when(notificationMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<Notification> result = notificationService.page(USER_ID, 2, 5, false);

        assertEquals(7, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(2, result.getPage());
        assertEquals(5, result.getPageSize());
    }

    @Test
    @DisplayName("未读数直接返回 count 查询结果")
    void unreadCount_returnsCount() {
        when(notificationMapper.selectCount(any())).thenReturn(4L);

        assertEquals(4L, notificationService.unreadCount(USER_ID));
    }

    @Test
    @DisplayName("标记单条已读：调用带 user_id 条件的专用更新")
    void markRead_updatesScopedToUser() {
        notificationService.markRead(10L, USER_ID);

        verify(notificationMapper).markRead(10L, USER_ID);
    }

    @Test
    @DisplayName("全部已读：按用户批量更新")
    void markAllRead_updatesScopedToUser() {
        notificationService.markAllRead(USER_ID);

        verify(notificationMapper).markAllRead(USER_ID);
    }
}