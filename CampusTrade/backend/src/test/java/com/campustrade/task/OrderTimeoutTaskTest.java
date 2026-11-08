package com.campustrade.task;

import com.campustrade.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderTimeoutTask 单元测试：扫描参数透传、逐单取消、单笔异常不影响后续订单。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTimeoutTask 单元测试")
class OrderTimeoutTaskTest {

    @Mock
    private OrderService orderService;

    private OrderTimeoutTask task;

    @BeforeEach
    void setUp() {
        task = new OrderTimeoutTask(orderService);
        ReflectionTestUtils.setField(task, "timeoutMinutes", 30);
        ReflectionTestUtils.setField(task, "scanIntervalMs", 300000L);
        ReflectionTestUtils.setField(task, "batchSize", 2);
    }

    @Test
    @DisplayName("没有超时订单时不调用取消")
    void noTimeoutOrders_doesNothing() {
        when(orderService.findTimeoutUnpaidOrderIds(30, 2)).thenReturn(List.of());

        task.cancelTimeoutOrders();

        verify(orderService, never()).cancelByTimeout(anyLong(), anyInt());
    }

    @Test
    @DisplayName("候选订单逐个取消，参数为配置的超时分钟")
    void cancelsEachCandidate() {
        when(orderService.findTimeoutUnpaidOrderIds(30, 2)).thenReturn(List.of(7L, 8L));
        when(orderService.cancelByTimeout(7L, 30)).thenReturn(true);
        when(orderService.cancelByTimeout(8L, 30)).thenReturn(false);

        task.cancelTimeoutOrders();

        InOrder order = inOrder(orderService);
        order.verify(orderService).cancelByTimeout(7L, 30);
        order.verify(orderService).cancelByTimeout(8L, 30);
    }

    @Test
    @DisplayName("单笔取消抛异常时继续处理后续订单")
    void oneFailure_doesNotStopOthers() {
        when(orderService.findTimeoutUnpaidOrderIds(30, 2)).thenReturn(List.of(7L, 8L));
        when(orderService.cancelByTimeout(7L, 30)).thenThrow(new RuntimeException("db deadlock"));
        when(orderService.cancelByTimeout(8L, 30)).thenReturn(true);

        task.cancelTimeoutOrders();

        verify(orderService).cancelByTimeout(8L, 30);
    }
}