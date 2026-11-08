package com.campustrade.service.impl;

import com.campustrade.cache.CacheKeys;
import com.campustrade.cache.CacheService;
import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.Goods;
import com.campustrade.entity.Order;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单模块单元测试
 * <p>
 * 覆盖：下单（含并发抢占失败）、取消、模拟支付、发货、确认收货、退款申请/同意/拒绝。
 * 所有状态流转均校验 CAS 条件更新的影响行数，保证并发重复操作只会成功一次。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 单元测试")
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private CacheService cacheService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.campustrade.service.WalletService walletService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private OrderServiceImpl orderService;

    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 1L;
    private static final Long GOODS_ID = 100L;
    private static final Long ORDER_ID = 200L;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderMapper, goodsMapper, cacheService, notificationService,
                walletService, eventPublisher);
    }

    /** 构造一个在售商品 */
    private Goods onSaleGoods() {
        Goods goods = new Goods();
        goods.setId(GOODS_ID);
        goods.setSellerId(SELLER_ID);
        goods.setTitle("二手自行车");
        goods.setPrice(new BigDecimal("150.00"));
        goods.setStatus(StatusConstant.GOODS_ON_SALE);
        return goods;
    }

    /** 构造一个待付款订单 */
    private Order pendingOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("CT20240101000001");
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setGoodsId(GOODS_ID);
        order.setAmount(new BigDecimal("150.00"));
        order.setStatus(StatusConstant.ORDER_PENDING_PAYMENT);
        order.setRefundStatus(0);
        return order;
    }

    @Nested
    @DisplayName("创建订单")
    class CreateOrderTests {

        @Test
        @DisplayName("下单成功：抢占商品、落库订单并通知卖家")
        void createOrder_success() {
            when(goodsMapper.selectById(GOODS_ID)).thenReturn(onSaleGoods());
            when(goodsMapper.markSold(GOODS_ID)).thenReturn(1);

            Order order = orderService.createOrder(BUYER_ID, GOODS_ID, "尽快发货");

            verify(goodsMapper).markSold(GOODS_ID);
            verify(orderMapper).insert(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertEquals(BUYER_ID, saved.getBuyerId());
            assertEquals(SELLER_ID, saved.getSellerId());
            assertEquals(GOODS_ID, saved.getGoodsId());
            assertEquals(new BigDecimal("150.00"), saved.getAmount());
            assertEquals(StatusConstant.ORDER_PENDING_PAYMENT, saved.getStatus());
            assertEquals("尽快发货", saved.getRemark());
            assertNotNull(saved.getOrderNo());
            assertTrue(saved.getOrderNo().startsWith("CT"));
            assertSame(saved, order);

            // 商品已售出，详情缓存必须被清除
            verify(cacheService).evict(CacheKeys.GOODS_DETAIL + GOODS_ID);
            // 卖家收到新订单通知
            verify(notificationService).send(eq(SELLER_ID), eq("ORDER_CREATED"), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void createOrder_goodsNotFound() {
            when(goodsMapper.selectById(GOODS_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.createOrder(BUYER_ID, GOODS_ID, null));
            assertEquals(ErrorCode.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("商品已下架/已售出时抛出异常")
        void createOrder_goodsOffline() {
            Goods goods = onSaleGoods();
            goods.setStatus(StatusConstant.GOODS_SOLD);
            when(goodsMapper.selectById(GOODS_ID)).thenReturn(goods);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.createOrder(BUYER_ID, GOODS_ID, null));
            assertEquals(ErrorCode.GOODS_OFFLINE.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("不能购买自己的商品")
        void createOrder_buyOwnGoods() {
            when(goodsMapper.selectById(GOODS_ID)).thenReturn(onSaleGoods());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.createOrder(SELLER_ID, GOODS_ID, null));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("并发抢占失败（商品已被他人锁定）时不落库订单")
        void createOrder_concurrentLockFailed() {
            when(goodsMapper.selectById(GOODS_ID)).thenReturn(onSaleGoods());
            // 另一个线程已把商品状态从 1 改为 3，CAS 影响 0 行
            when(goodsMapper.markSold(GOODS_ID)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.createOrder(BUYER_ID, GOODS_ID, null));
            assertEquals(ErrorCode.GOODS_SOLD.getCode(), ex.getCode());

            verify(orderMapper, never()).insert(any(Order.class));
            verify(notificationService, never()).send(anyLong(), anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("取消订单")
    class CancelOrderTests {

        @Test
        @DisplayName("取消成功：恢复商品在售并通知卖家")
        void cancel_success() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.updateStatus(ORDER_ID, StatusConstant.ORDER_PENDING_PAYMENT,
                    StatusConstant.ORDER_CANCELLED)).thenReturn(1);

            orderService.cancelOrder(ORDER_ID, BUYER_ID);

            verify(goodsMapper).restoreOnSale(GOODS_ID);
            verify(cacheService).evict(CacheKeys.GOODS_DETAIL + GOODS_ID);
            verify(notificationService).send(eq(SELLER_ID), eq("ORDER_CANCELLED"), anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("订单已支付/已取消时无法取消")
        void cancel_statusInvalid() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.updateStatus(anyLong(), anyInt(), anyInt())).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.cancelOrder(ORDER_ID, BUYER_ID));
            assertEquals(ErrorCode.ORDER_CANNOT_CANCEL.getCode(), ex.getCode());
            verify(goodsMapper, never()).restoreOnSale(anyLong());
        }

        @Test
        @DisplayName("非买家不能取消")
        void cancel_notBuyer() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.cancelOrder(ORDER_ID, 999L));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("模拟支付")
    class PayOrderTests {

        @Test
        @DisplayName("支付成功：待付款 -> 已付款并通知卖家")
        void pay_success() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.updateStatus(ORDER_ID, StatusConstant.ORDER_PENDING_PAYMENT,
                    StatusConstant.ORDER_PAID)).thenReturn(1);

            orderService.payOrder(ORDER_ID, BUYER_ID);

            verify(notificationService).send(eq(SELLER_ID), eq("ORDER_PAID"), anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("重复支付时 CAS 影响 0 行，抛出无法支付")
        void pay_duplicate() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.updateStatus(anyLong(), anyInt(), anyInt())).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.payOrder(ORDER_ID, BUYER_ID));
            assertEquals(ErrorCode.ORDER_CANNOT_PAY.getCode(), ex.getCode());
            verify(notificationService, never()).send(anyLong(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("非买家不能支付")
        void pay_notBuyer() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.payOrder(ORDER_ID, 999L));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("卖家发货")
    class ShipOrderTests {

        private Order paidOrder() {
            Order order = pendingOrder();
            order.setStatus(StatusConstant.ORDER_PAID);
            return order;
        }

        @Test
        @DisplayName("发货成功：已付款 -> 已发货并通知买家")
        void ship_success() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(paidOrder());
            when(orderMapper.updateStatus(ORDER_ID, StatusConstant.ORDER_PAID,
                    StatusConstant.ORDER_SHIPPED)).thenReturn(1);

            orderService.shipOrder(ORDER_ID, SELLER_ID);

            verify(notificationService).send(eq(BUYER_ID), eq("ORDER_SHIPPED"), anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("非卖家不能发货")
        void ship_notSeller() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(paidOrder());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.shipOrder(ORDER_ID, BUYER_ID));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("退款处理中不允许发货")
        void ship_refundProcessing() {
            Order order = paidOrder();
            order.setRefundStatus(1);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.shipOrder(ORDER_ID, SELLER_ID));
            assertEquals(ErrorCode.ORDER_REFUND_PROCESSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("订单未支付时无法发货")
        void ship_statusInvalid() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.updateStatus(anyLong(), anyInt(), anyInt())).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.shipOrder(ORDER_ID, SELLER_ID));
            assertEquals(ErrorCode.ORDER_CANNOT_SHIP.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("确认收货")
    class ConfirmReceiptTests {

        @Test
        @DisplayName("已发货 -> 已收货，通知卖家")
        void confirm_fromShipped() {
            Order order = pendingOrder();
            order.setStatus(StatusConstant.ORDER_SHIPPED);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.updateStatus(ORDER_ID, StatusConstant.ORDER_SHIPPED,
                    StatusConstant.ORDER_RECEIVED)).thenReturn(1);

            orderService.confirmReceipt(ORDER_ID, BUYER_ID);

            verify(notificationService).send(eq(SELLER_ID), eq("ORDER_RECEIVED"), anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("兼容卖家未发货：已付款 -> 已收货")
        void confirm_fromPaidFallback() {
            Order order = pendingOrder();
            order.setStatus(StatusConstant.ORDER_PAID);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.updateStatus(ORDER_ID, StatusConstant.ORDER_SHIPPED,
                    StatusConstant.ORDER_RECEIVED)).thenReturn(0);
            when(orderMapper.updateStatus(ORDER_ID, StatusConstant.ORDER_PAID,
                    StatusConstant.ORDER_RECEIVED)).thenReturn(1);

            orderService.confirmReceipt(ORDER_ID, BUYER_ID);

            verify(notificationService).send(eq(SELLER_ID), eq("ORDER_RECEIVED"), anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("状态不允许时抛出无法确认收货")
        void confirm_statusInvalid() {
            Order order = pendingOrder();
            order.setStatus(StatusConstant.ORDER_CANCELLED);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.updateStatus(anyLong(), anyInt(), anyInt())).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.confirmReceipt(ORDER_ID, BUYER_ID));
            assertEquals(ErrorCode.ORDER_CANNOT_CONFIRM.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("退款处理中不允许确认收货")
        void confirm_refundProcessing() {
            Order order = pendingOrder();
            order.setStatus(StatusConstant.ORDER_SHIPPED);
            order.setRefundStatus(1);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.confirmReceipt(ORDER_ID, BUYER_ID));
            assertEquals(ErrorCode.ORDER_REFUND_PROCESSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非买家不能确认收货")
        void confirm_notBuyer() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.confirmReceipt(ORDER_ID, SELLER_ID));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("退款")
    class RefundTests {

        private Order paidOrder() {
            Order order = pendingOrder();
            order.setStatus(StatusConstant.ORDER_PAID);
            return order;
        }

        @Test
        @DisplayName("买家申请退款成功并通知卖家")
        void applyRefund_success() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(paidOrder());
            when(orderMapper.applyRefund(ORDER_ID, "商品与描述不符")).thenReturn(1);

            orderService.applyRefund(ORDER_ID, BUYER_ID, "商品与描述不符");

            verify(notificationService).send(eq(SELLER_ID), eq("REFUND_APPLY"), anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("订单状态不允许退款时抛出异常")
        void applyRefund_notAllowed() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.applyRefund(anyLong(), any())).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.applyRefund(ORDER_ID, BUYER_ID, null));
            assertEquals(ErrorCode.ORDER_CANNOT_REFUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("卖家同意退款：订单取消、商品恢复在售并通知买家")
        void agreeRefund_success() {
            Order order = paidOrder();
            order.setRefundStatus(1);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.agreeRefund(ORDER_ID)).thenReturn(1);

            orderService.agreeRefund(ORDER_ID, SELLER_ID);

            verify(goodsMapper).restoreOnSale(GOODS_ID);
            verify(cacheService).evict(CacheKeys.GOODS_DETAIL + GOODS_ID);
            verify(notificationService).send(eq(BUYER_ID), eq("REFUND_AGREED"), anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("无待处理退款申请时同意退款失败")
        void agreeRefund_noPending() {
            Order order = paidOrder();
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.agreeRefund(ORDER_ID)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.agreeRefund(ORDER_ID, SELLER_ID));
            assertEquals(ErrorCode.ORDER_CANNOT_AGREE_REFUND.getCode(), ex.getCode());
            verify(goodsMapper, never()).restoreOnSale(anyLong());
        }

        @Test
        @DisplayName("非卖家不能同意退款")
        void agreeRefund_notSeller() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(paidOrder());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.agreeRefund(ORDER_ID, BUYER_ID));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("卖家拒绝退款并通知买家")
        void rejectRefund_success() {
            Order order = paidOrder();
            order.setRefundStatus(1);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.rejectRefund(ORDER_ID)).thenReturn(1);

            orderService.rejectRefund(ORDER_ID, SELLER_ID);

            verify(notificationService).send(eq(BUYER_ID), eq("REFUND_REJECTED"), anyString(), anyString(), eq(ORDER_ID));
            verify(goodsMapper, never()).restoreOnSale(anyLong());
        }

        @Test
        @DisplayName("无待处理退款申请时拒绝退款失败")
        void rejectRefund_noPending() {
            Order order = paidOrder();
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
            when(orderMapper.rejectRefund(ORDER_ID)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.rejectRefund(ORDER_ID, SELLER_ID));
            assertEquals(ErrorCode.ORDER_CANNOT_REJECT_REFUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("查询")
    class QueryTests {

        @Test
        @DisplayName("买卖双方均可查看订单详情")
        void detail_okForBuyerAndSeller() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());

            assertNotNull(orderService.getOrderDetail(ORDER_ID, BUYER_ID));
            assertNotNull(orderService.getOrderDetail(ORDER_ID, SELLER_ID));
        }

        @Test
        @DisplayName("第三方不能查看订单详情")
        void detail_forbidden() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.getOrderDetail(ORDER_ID, 999L));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void detail_notFound() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderService.getOrderDetail(ORDER_ID, BUYER_ID));
            assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("超时未支付自动取消")
    class TimeoutCancelTests {

        @Test
        @DisplayName("扫描时按「当前时间 - 超时分钟」计算截止时间并透传批量上限")
        void findTimeoutIds_delegates() {
            when(orderMapper.selectTimeoutUnpaidIds(any(LocalDateTime.class), eq(50)))
                    .thenReturn(List.of(ORDER_ID));

            List<Long> ids = orderService.findTimeoutUnpaidOrderIds(30, 50);

            assertEquals(List.of(ORDER_ID), ids);
            ArgumentCaptor<LocalDateTime> deadlineCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(orderMapper).selectTimeoutUnpaidIds(deadlineCaptor.capture(), eq(50));
            long diffSeconds = Math.abs(Duration.between(
                    deadlineCaptor.getValue(), LocalDateTime.now().minusMinutes(30)).toSeconds());
            assertTrue(diffSeconds < 5, "截止时间应约等于当前时间减 30 分钟，实际偏差 " + diffSeconds + " 秒");
        }

        @Test
        @DisplayName("Mapper 返回 null 时安全返回空集合")
        void findTimeoutIds_nullSafe() {
            when(orderMapper.selectTimeoutUnpaidIds(any(LocalDateTime.class), anyInt())).thenReturn(null);

            assertTrue(orderService.findTimeoutUnpaidOrderIds(30, 100).isEmpty());
        }

        @Test
        @DisplayName("取消成功：CAS 0->4、商品恢复在售、清缓存并通知买卖双方")
        void cancelByTimeout_success() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.updateStatus(ORDER_ID, StatusConstant.ORDER_PENDING_PAYMENT,
                    StatusConstant.ORDER_CANCELLED)).thenReturn(1);

            assertTrue(orderService.cancelByTimeout(ORDER_ID, 30));

            verify(goodsMapper).restoreOnSale(GOODS_ID);
            verify(cacheService).evict(CacheKeys.GOODS_DETAIL + GOODS_ID);
            verify(notificationService).send(eq(BUYER_ID), eq("ORDER_TIMEOUT_CANCELLED"),
                    anyString(), anyString(), eq(ORDER_ID));
            verify(notificationService).send(eq(SELLER_ID), eq("ORDER_TIMEOUT_CANCELLED"),
                    anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("订单已被支付时 CAS 影响 0 行：不取消、不动商品、不发通知")
        void cancelByTimeout_alreadyPaid() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(pendingOrder());
            when(orderMapper.updateStatus(anyLong(), anyInt(), anyInt())).thenReturn(0);

            assertFalse(orderService.cancelByTimeout(ORDER_ID, 30));

            verify(goodsMapper, never()).restoreOnSale(anyLong());
            verify(notificationService, never()).send(anyLong(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("订单不存在时返回 false")
        void cancelByTimeout_notFound() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

            assertFalse(orderService.cancelByTimeout(ORDER_ID, 30));
        }
    }
}