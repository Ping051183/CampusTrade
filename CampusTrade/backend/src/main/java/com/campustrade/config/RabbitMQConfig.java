package com.campustrade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：TTL + 死信交换机实现「延迟队列」。
 * <p>
 * 为什么不用 x-delayed-message 插件：需要运维在 broker 上装插件，换环境就得再装一次；
 * TTL + DLX 是纯应用侧配置，官方镜像开箱即用。
 * <p>
 * 链路：下单 → order.delay.exchange → order.delay.queue（不消费，只等 TTL 到期）
 * → 过期后按 x-dead-letter-* 投到 order.timeout.exchange → order.timeout.queue → 消费者取消订单。
 * <p>
 * 注意：RabbitMQ 只会在消息到达队头时检查过期，因此同一队列里的 TTL 必须一致
 * （本项目的超时时间全局统一，满足这一点；如果将来按订单类型设置不同 TTL，应改用延迟插件）。
 */
@Configuration
public class RabbitMQConfig {

    /** 延迟交换机：只负责把消息投进延迟队列 */
    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final String DELAY_ROUTING_KEY = "order.delay";

    /** 死信交换机：延迟队列里过期消息的出口 */
    public static final String TIMEOUT_EXCHANGE = "order.timeout.exchange";
    public static final String TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String TIMEOUT_ROUTING_KEY = "order.timeout";

    /** 延迟队列堆积上限，超过后拒绝新消息，避免 broker 被大量未到期消息打爆 */
    private static final int DELAY_QUEUE_MAX_LENGTH = 100_000;

    /** 死信原因里带上原队列名，便于排查 */
    private static final String X_DEATH_HEADER = "x-death";

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .deadLetterExchange(TIMEOUT_EXCHANGE)
                .deadLetterRoutingKey(TIMEOUT_ROUTING_KEY)
                .maxLength(DELAY_QUEUE_MAX_LENGTH)
                .overflow(QueueBuilder.Overflow.rejectPublish)
                .build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderTimeoutExchange() {
        return new DirectExchange(TIMEOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(TIMEOUT_QUEUE)
                // 消费失败不重回队列：避免毒消息无限重投，漏掉的订单由定时任务兜底
                .build();
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderTimeoutExchange()).with(TIMEOUT_ROUTING_KEY);
    }

    /**
     * 复用 Spring 的 ObjectMapper（带 JavaTimeModule 与 ParameterNamesModule），
     * 否则 LocalDateTime 字段会序列化失败。
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}