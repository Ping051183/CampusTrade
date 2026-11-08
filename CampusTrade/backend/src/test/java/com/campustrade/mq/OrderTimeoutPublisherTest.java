package com.campustrade.mq;

import com.campustrade.config.RabbitMQConfig;
import com.campustrade.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderTimeoutPublisher 单元测试：投递参数、TTL、失败降级、开关。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单超时消息投递")
class OrderTimeoutPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderTimeoutPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OrderTimeoutPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "timeoutMinutes", 30);
        ReflectionTestUtils.setField(publisher, "mqEnabled", true);
    }

    @Test
    @DisplayName("投递到延迟交换机，TTL 等于超时分钟数")
    void publishWithTtl() {
        OrderCreatedEvent event = new OrderCreatedEvent(2001L, LocalDateTime.now());

        publisher.onOrderCreated(event);

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DELAY_EXCHANGE),
                eq(RabbitMQConfig.DELAY_ROUTING_KEY),
                any(OrderTimeoutMessage.class),
                captor.capture());

        Message message = MessageBuilder.withBody(new byte[0]).build();
        captor.getValue().postProcessMessage(message);
        assertEquals("1800000", message.getMessageProperties().getExpiration());
    }

    @Test
    @DisplayName("Broker 不可用时只记日志并计数，不影响下单主流程")
    void degradeWhenBrokerDown() {
        doThrow(new AmqpConnectException(new RuntimeException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        assertDoesNotThrow(() -> publisher.onOrderCreated(new OrderCreatedEvent(2002L, LocalDateTime.now())));
        assertEquals(1L, publisher.getFailureCount());
    }

    @Test
    @DisplayName("关闭 MQ 开关后不再投递")
    void skipWhenDisabled() {
        ReflectionTestUtils.setField(publisher, "mqEnabled", false);

        publisher.onOrderCreated(new OrderCreatedEvent(2003L, LocalDateTime.now()));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
        assertEquals(0L, publisher.getFailureCount());
    }
}