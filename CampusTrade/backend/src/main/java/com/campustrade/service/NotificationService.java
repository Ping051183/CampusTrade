package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.entity.Notification;

/**
 * 系统通知Service
 */
public interface NotificationService {

    /**
     * 发送一条通知
     *
     * @param userId    接收人
     * @param type      通知类型
     * @param title     标题
     * @param content   内容
     * @param relatedId 关联业务ID（订单ID等，可为 null）
     */
    void send(Long userId, String type, String title, String content, Long relatedId);

    /**
     * 分页查询我的通知
     */
    PageResult<Notification> page(Long userId, int page, int pageSize, boolean unreadOnly);

    /**
     * 未读通知数
     */
    long unreadCount(Long userId);

    /**
     * 标记单条已读
     */
    void markRead(Long id, Long userId);

    /**
     * 全部标记已读
     */
    void markAllRead(Long userId);
}