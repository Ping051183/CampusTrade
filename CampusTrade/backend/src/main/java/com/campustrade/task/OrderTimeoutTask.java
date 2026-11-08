package com.campustrade.task;

import com.campustrade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时自动取消任务
 * <p>
 * 背景：下单时商品会被原子锁定为「已售出」，如果买家一直不付款、也不主动取消，
 * 商品就会一直处于不可购买状态。本任务周期扫描超过 N 分钟仍未支付的订单，
 * 逐单取消（CAS 0 -> 4）并恢复商品在售，同时通知买卖双方。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "campustrade.order.scan-enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutTask {

    private final OrderService orderService;

    /** 待付款订单超时时间（分钟） */
    @Value("${campustrade.order.timeout-minutes:30}")
    private int timeoutMinutes;

    /** 扫描间隔（毫秒） */
    @Value("${campustrade.order.scan-interval-ms:300000}")
    private long scanIntervalMs;

    /** 单批最大处理数量，避免一次扫描锁太多行 */
    @Value("${campustrade.order.scan-batch-size:100}")
    private int batchSize;

    /**
     * 周期扫描超时未支付订单。
     * <p>
     * 每单独立调用 {@link OrderService#cancelByTimeout(Long, int)}（独立事务），
     * 单笔失败只记录日志，不影响其余订单。
     */
    @Scheduled(initialDelayString = "${campustrade.order.initial-delay-ms:60000}",
            fixedDelayString = "${campustrade.order.scan-interval-ms:300000}")
    public void cancelTimeoutOrders() {
        List<Long> ids = orderService.findTimeoutUnpaidOrderIds(timeoutMinutes, batchSize);
        if (ids.isEmpty()) {
            return;
        }

        int cancelled = 0;
        for (Long orderId : ids) {
            try {
                if (orderService.cancelByTimeout(orderId, timeoutMinutes)) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.warn("自动取消超时订单失败，orderId={}，原因：{}", orderId, e.getMessage());
            }
        }
        log.info("超时订单扫描完成：候选 {} 单，实际取消 {} 单（超时阈值 {} 分钟，扫描间隔 {} ms）",
                ids.size(), cancelled, timeoutMinutes, scanIntervalMs);
    }
}