package com.campustrade.mq;

import java.time.LocalDateTime;

/**
 * 订单超时消息体。
 *
 * @param orderId 订单主键
 * @param expiresAt 期望的取消时间点（建单时间 + 超时分钟数），消费端据此防提前取消
 */
public record OrderTimeoutMessage(Long orderId, LocalDateTime expiresAt) {
}