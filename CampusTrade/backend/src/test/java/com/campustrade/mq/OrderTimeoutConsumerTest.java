package com.campustrade.mq;

import com.campustrade.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderTimeoutConsumer 单元测试：正常取消、已支付跳过、提前到达跳过、异常不外抛。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单超时消息消费者")
class OrderTimeoutConsumerTest {

    @Mock
    private OrderService orderService;

    private OrderTimeoutConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderTimeoutConsumer(orderService);
        ReflectionTestUtils.setField(consumer, "timeoutMinutes", 30);
    }

    @Test
    @DisplayName("消息到期后取消未支付订单")
    void cancelExpiredOrder() {
        when(orderService.cancelByTimeout(eq(1001L), anyInt())).thenReturn(true);
        OrderTimeoutMessage message = new OrderTimeoutMessage(1001L, LocalDateTime.now().minusMinutes(1));

        consumer.onTimeoutMessage(message);

        verify(orderService).cancelByTimeout(1001L, 30);
    }

    @Test
    @DisplayName("订单已支付时静默跳过，不报错")
    void skipPaidOrder() {
        when(orderService.cancelByTimeout(eq(1002L), anyInt())).thenReturn(false);
        OrderTimeoutMessage message = new OrderTimeoutMessage(1002L, LocalDateTime.now().minusMinutes(1));

        consumer.onTimeoutMessage(message);

        verify(orderService).cancelByTimeout(1002L, 30);
    }

    @Test
    @DisplayName("消息提前到达时不取消，交给定时任务兜底")
    void skipEarlyMessage() {
        OrderTimeoutMessage message = new OrderTimeoutMessage(1003L, LocalDateTime.now().plusMinutes(10));

        consumer.onTimeoutMessage(message);

        verify(orderService, never()).cancelByTimeout(eq(1003L), anyInt());
    }

    @Test
    @DisplayName("业务异常被吞掉，避免毒消息无限重投")
    void swallowException() {
        when(orderService.cancelByTimeout(eq(1004L), anyInt())).thenThrow(new IllegalStateException("db down"));
        OrderTimeoutMessage message = new OrderTimeoutMessage(1004L, LocalDateTime.now().minusMinutes(1));

        assertDoesNotThrow(() -> consumer.onTimeoutMessage(message));
    }

    @Test
    @DisplayName("空消息直接丢弃")
    void ignoreNullMessage() {
        assertDoesNotThrow(() -> consumer.onTimeoutMessage(null));
        verify(orderService, never()).cancelByTimeout(org.mockito.ArgumentMatchers.anyLong(), anyInt());
    }
}