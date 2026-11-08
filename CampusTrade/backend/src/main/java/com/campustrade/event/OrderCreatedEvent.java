package com.campustrade.event;

/**
 * 订单创建事件。
 * <p>
 * 下单成功后由 Service 层发布，监听方在事务提交后（AFTER_COMMIT）才真正投递 MQ，
 * 避免"消息发出去了但订单回滚了"的脏投递。
 *
 * @param orderId 订单主键
 * @param createdAt 建单时间，消费端据此判断是否真的到点了
 */
public record OrderCreatedEvent(Long orderId, java.time.LocalDateTime createdAt) {
}