package com.campustrade.mq;

import com.campustrade.config.RabbitMQConfig;
import com.campustrade.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 下单后投递「超时取消」延迟消息。
 * <p>
 * 只在事务提交后发送（AFTER_COMMIT）：事务回滚时不会有消息漏到队列里。
 * 发送失败只记日志并计数，不阻断下单主流程——定时任务仍在兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutPublisher {

    private final RabbitTemplate rabbitTemplate;

    /** 待付款超时时间（分钟），与 OrderTimeoutTask 共用同一份配置 */
    @Value("${campustrade.order.timeout-minutes:30}")
    private int timeoutMinutes;

    /** 总开关：关掉后退回纯定时任务模式 */
    @Value("${campustrade.order.mq-enabled:true}")
    private boolean mqEnabled;

    /** 投递失败计数，可通过 /actuator/metrics 之外的方式快速自查 */
    private final AtomicLong failureCount = new AtomicLong();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (!mqEnabled) {
            return;
        }
        Long orderId = event.orderId();
        try {
            OrderTimeoutMessage payload = new OrderTimeoutMessage(
                    orderId, event.createdAt().plusMinutes(timeoutMinutes));
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELAY_EXCHANGE,
                    RabbitMQConfig.DELAY_ROUTING_KEY,
                    payload,
                    ttlPostProcessor());
        } catch (AmqpException e) {
            failureCount.incrementAndGet();
            log.warn("订单超时消息投递失败，orderId={}，将由定时任务兜底：{}", orderId, e.getMessage());
        }
    }

    /**
     * 每条消息单独设置 TTL：改配置后对新订单立即生效，
     * 不需要重建队列（队列级 x-message-ttl 在队列创建后就改不了）。
     */
    MessagePostProcessor ttlPostProcessor() {
        long ttlMillis = timeoutMinutes * 60_000L;
        return message -> {
            message.getMessageProperties().setExpiration(String.valueOf(ttlMillis));
            return message;
        };
    }

    public long getFailureCount() {
        return failureCount.get();
    }
}