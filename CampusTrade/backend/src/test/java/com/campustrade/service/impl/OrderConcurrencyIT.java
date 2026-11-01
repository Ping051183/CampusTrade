package com.campustrade.service.impl;

import com.campustrade.CampusTradeApplication;
import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.Order;
import com.campustrade.exception.BusinessException;
import com.campustrade.service.OrderService;
import com.campustrade.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单并发集成测试（需要本地 MySQL 中已存在 campus_trade 库和 v2 迁移表结构）
 * <p>
 * 默认不随 mvn test 执行，运行方式：
 * <pre>
 *   mvn test -Dtest=OrderConcurrencyIT -Dit=true
 * </pre>
 * 验证内容：
 * 1. 同一商品被多个买家并发下单时，只有 1 人成功，其余全部失败，数据库只有 1 条订单，商品被置为已售出；
 * 2. 完整生命周期（下单 → 支付 → 发货 → 确认收货 → 评价）在真实数据库上可用。
 */
@SpringBootTest(classes = CampusTradeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "it", matches = "true",
        disabledReason = "集成测试需要本地 MySQL，使用 mvn test -Dtest=OrderConcurrencyIT -Dit=true 运行")
@DisplayName("订单并发集成测试")
class OrderConcurrencyIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JdbcTemplate jdbc;

    private static final Long SELLER_ID = 1L;
    private static final Long BUYER_ID = 2L;

    private Long goodsId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO goods (title, description, category_id, price, seller_id, images, status) "
                + "VALUES ('并发测试商品', '集成测试自动创建，测试结束自动清理', 1, 9.90, ?, '[]', 1)", SELLER_ID);
        goodsId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertNotNull(goodsId);
    }

    @AfterEach
    void tearDown() {
        // 先清理订单关联的评价和通知，再删订单和商品
        List<Long> orderIds = jdbc.queryForList("SELECT id FROM orders WHERE goods_id = ?", Long.class, goodsId);
        if (!orderIds.isEmpty()) {
            String placeholders = orderIds.stream().map(id -> "?").collect(Collectors.joining(","));
            jdbc.update("DELETE FROM review WHERE order_id IN (" + placeholders + ")", orderIds.toArray());
            jdbc.update("DELETE FROM notification WHERE related_id IN (" + placeholders + ")", orderIds.toArray());
            jdbc.update("DELETE FROM orders WHERE goods_id = ?", goodsId);
        }
        jdbc.update("DELETE FROM goods WHERE id = ?", goodsId);
    }

    @Test
    @DisplayName("10 个买家并发下单，只有 1 单成功，不产生超卖")
    void concurrentCreateOrder_onlyOneSucceeds() throws Exception {
        int buyers = 10;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch ready = new CountDownLatch(buyers);
        CountDownLatch start = new CountDownLatch(1);
        List<String> failures = new CopyOnWriteArrayList<>();
        List<Order> successes = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < buyers; i++) {
                long buyerId = BUYER_ID + i;
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        successes.add(orderService.createOrder(buyerId, goodsId, "并发下单"));
                    } catch (BusinessException e) {
                        // 抢占失败的请求必须拿到明确的业务错误（商品已售出）
                        failures.add(e.getCode() + ":" + e.getMessage());
                    } catch (Exception e) {
                        failures.add("UNEXPECTED:" + e.getClass().getSimpleName() + ":" + e.getMessage());
                    }
                });
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "并发下单任务未在 60 秒内结束");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, successes.size(), "并发下单必须只有一笔成功，实际成功 " + successes.size() + " 笔");
        assertEquals(buyers - 1, failures.size(), "其余请求都应失败");
        assertTrue(failures.stream().noneMatch(f -> f.startsWith("UNEXPECTED")),
                "失败请求出现了非预期的异常：" + failures);

        Integer orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE goods_id = ?", Integer.class, goodsId);
        assertEquals(1, orderCount, "数据库中同一商品只能有 1 条订单");

        Integer goodsStatus = jdbc.queryForObject("SELECT status FROM goods WHERE id = ?", Integer.class, goodsId);
        assertEquals(StatusConstant.GOODS_SOLD, goodsStatus, "下单成功后商品应被置为已售出");
    }

    @Test
    @DisplayName("真实数据库上的完整交易生命周期：下单→支付→发货→收货→评价")
    void fullTradeLifecycle() {
        Order order = orderService.createOrder(BUYER_ID, goodsId, "集成测试订单");
        assertEquals(StatusConstant.ORDER_PENDING_PAYMENT, order.getStatus());

        orderService.payOrder(order.getId(), BUYER_ID);
        assertEquals(StatusConstant.ORDER_PAID, status(order.getId()));

        orderService.shipOrder(order.getId(), SELLER_ID);
        assertEquals(StatusConstant.ORDER_SHIPPED, status(order.getId()));

        orderService.confirmReceipt(order.getId(), BUYER_ID);
        assertEquals(StatusConstant.ORDER_RECEIVED, status(order.getId()));

        // 双方各评一次：买家评卖家
        reviewService.createReview(order.getId(), BUYER_ID, 5, "交易顺利");
        Integer rated = jdbc.queryForObject("SELECT is_rated FROM orders WHERE id = ?", Integer.class, order.getId());
        assertEquals(1, rated);

        // 重复评价被拒绝
        assertThrows(BusinessException.class, () -> reviewService.createReview(order.getId(), BUYER_ID, 4, "再评一次"));
    }

    private Integer status(Long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", Integer.class, orderId);
    }
}