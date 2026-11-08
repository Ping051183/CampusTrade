package com.campustrade.controller;

import com.campustrade.entity.Order;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.exception.GlobalExceptionHandler;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrderController 接口测试（MockMvc）：验证路由、当前用户解析、统一返回结构与全局异常映射。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController 接口测试")
class OrderControllerTest {

    private static final Long USER_ID = 2L;
    private static final Long ORDER_ID = 5L;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderMapper orderMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(orderService, orderMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        // 模拟 JWT 过滤器写入的登录态：principal 即当前用户 ID
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("支付 / 发货 / 确认收货")
    class TradeFlowTests {

        @Test
        @DisplayName("支付成功返回 200 与提示信息")
        void pay_success() throws Exception {
            mockMvc.perform(put("/api/order/pay/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("支付成功"));

            verify(orderService).payOrder(ORDER_ID, USER_ID);
        }

        @Test
        @DisplayName("重复支付时业务异常 5005 以 HTTP 200 返回")
        void pay_conflict() throws Exception {
            doThrow(new BusinessException(ErrorCode.ORDER_CANNOT_PAY))
                    .when(orderService).payOrder(ORDER_ID, USER_ID);

            mockMvc.perform(put("/api/order/pay/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_CANNOT_PAY.getCode()));
        }

        @Test
        @DisplayName("卖家发货成功")
        void ship_success() throws Exception {
            mockMvc.perform(put("/api/order/ship/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("已发货"));

            verify(orderService).shipOrder(ORDER_ID, USER_ID);
        }

        @Test
        @DisplayName("买家确认收货成功")
        void confirm_success() throws Exception {
            mockMvc.perform(put("/api/order/confirm/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("已确认收货"));

            verify(orderService).confirmReceipt(ORDER_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("下单 / 退款")
    class CreateAndRefundTests {

        @Test
        @DisplayName("下单时把 remark 透传给 Service 并返回订单号")
        void createOrder_passesRemark() throws Exception {
            Order order = new Order();
            order.setId(ORDER_ID);
            order.setOrderNo("CT20240101000001");
            when(orderService.createOrder(USER_ID, 9L, "校门口自提")).thenReturn(order);

            mockMvc.perform(post("/api/order/create/{goodsId}", 9L).param("remark", "校门口自提"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("下单成功"))
                    .andExpect(jsonPath("$.data.orderNo").value("CT20240101000001"));

            verify(orderService).createOrder(USER_ID, 9L, "校门口自提");
        }

        @Test
        @DisplayName("商品不存在时返回 400 业务码")
        void createOrder_goodsNotFound() throws Exception {
            when(orderService.createOrder(any(), any(), any()))
                    .thenThrow(new BusinessException(ErrorCode.GOODS_NOT_FOUND));

            mockMvc.perform(post("/api/order/create/{goodsId}", 999L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.GOODS_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("申请退款把原因透传给 Service")
        void applyRefund_passesReason() throws Exception {
            mockMvc.perform(post("/api/order/refund/{id}", ORDER_ID).param("reason", "商品与描述不符"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(orderService).applyRefund(eq(ORDER_ID), eq(USER_ID), eq("商品与描述不符"));
        }

        @Test
        @DisplayName("订单详情不存在时返回 5001")
        void detail_notFound() throws Exception {
            when(orderService.getOrderDetail(ORDER_ID, USER_ID))
                    .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(get("/api/order/detail/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_NOT_FOUND.getCode()));
        }
    }
}