package com.campustrade.mq;

import com.campustrade.config.RabbitMQConfig;
import com.campustrade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消费「订单超时」消息，取消未支付订单并恢复商品在售。
 * <p>
 * 幂等性由下游 CAS（status 0 → 4）保证：删除队列是 at-least-once 语义，
 * 同一条消息重复投递时只有第一次能更新成功，其余直接返回 false。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    @Value("${campustrade.order.timeout-minutes:30}")
    private int timeoutMinutes;

    @RabbitListener(queues = RabbitMQConfig.TIMEOUT_QUEUE)
    public void onTimeoutMessage(OrderTimeoutMessage message) {
        if (message == null || message.orderId() == null) {
            log.warn("收到无效的订单超时消息，直接丢弃");
            return;
        }
        Long orderId = message.orderId();
        // 防御 Broker 重启、时钟回拨等导致的提前到达：此时不取消，交给定时任务兜底
        if (message.expiresAt() != null && LocalDateTime.now().isBefore(message.expiresAt())) {
            log.warn("订单超时消息提前到达，跳过取消，orderId={}，expiresAt={}", orderId, message.expiresAt());
            return;
        }
        try {
            boolean cancelled = orderService.cancelByTimeout(orderId, timeoutMinutes);
            if (cancelled) {
                log.info("订单超时自动取消成功，orderId={}", orderId);
            } else {
                log.debug("订单已支付或已取消，忽略超时消息，orderId={}", orderId);
            }
        } catch (Exception e) {
            // 不能向外抛：默认 auto ack 下抛异常会无限重投，形成毒消息
            log.error("处理订单超时消息失败，orderId={}，交由定时任务兜底", orderId, e);
        }
    }
}