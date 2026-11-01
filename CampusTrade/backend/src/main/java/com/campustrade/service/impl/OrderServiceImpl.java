package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.campustrade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单Service实现类
 * <p>
 * 状态流转（全部使用条件更新 CAS，防止并发重复操作）：
 * 0 待付款 --支付--> 1 已付款 --发货--> 2 已发货 --确认收货--> 3 已收货
 * 0 待付款 --取消--> 4 已取消（商品恢复在售）
 * 1/2 已付款或已发货 --买家申请退款--> 退款申请中
 * 退款申请中 --卖家同意--> 已退款（订单取消、商品恢复在售）
 * 退款申请中 --卖家拒绝--> 退款被拒绝（订单状态保持不变）
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final CacheService cacheService;
    private final NotificationService notificationService;

    /** 订单号生成计数器 */
    private static final AtomicLong ORDER_COUNTER = new AtomicLong(0);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long buyerId, Long goodsId, String remark) {
        // 查询商品
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.GOODS_NOT_FOUND);
        }
        if (!StatusConstant.GOODS_ON_SALE.equals(goods.getStatus())) {
            throw new BusinessException(ErrorCode.GOODS_OFFLINE);
        }
        if (goods.getSellerId().equals(buyerId)) {
            throw new BusinessException(400, "不能购买自己的商品");
        }

        // 原子抢占商品：UPDATE goods SET status=3 WHERE id=? AND status=1
        // 并发下单时只有第一条更新能成功（影响行数为 1），其余线程影响行数为 0，直接失败，
        // 从根上避免两个买家同时下单造成的超卖。
        int locked = goodsMapper.markSold(goodsId);
        if (locked == 0) {
            throw new BusinessException(ErrorCode.GOODS_SOLD, "手慢了，商品已被他人下单");
        }
        // 商品状态已变为已售出，删除详情缓存，避免列表/详情继续显示在售
        cacheService.evict(CacheKeys.GOODS_DETAIL + goodsId);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getSellerId());
        order.setGoodsId(goodsId);
        order.setAmount(goods.getPrice());
        order.setStatus(StatusConstant.ORDER_PENDING_PAYMENT);
        order.setIsRated(0);
        order.setSellerRated(0);
        order.setRefundStatus(0);
        order.setRemark(remark);
        orderMapper.insert(order);

        // 通知卖家
        notificationService.send(goods.getSellerId(), "ORDER_CREATED", "您有一笔新订单",
                "商品「" + goods.getTitle() + "」被买家下单，金额 ¥" + goods.getPrice() + "，请等待买家付款",
                order.getId());

        return order;
    }

    @Override
    public Order getOrderDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 只有买家和卖家可以查看订单详情
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 只有待付款订单可以取消，CAS 避免与支付并发时把已付款订单取消掉
        int rows = orderMapper.updateStatus(orderId,
                StatusConstant.ORDER_PENDING_PAYMENT, StatusConstant.ORDER_CANCELLED);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // 恢复商品为在售状态
        goodsMapper.restoreOnSale(order.getGoodsId());
        cacheService.evict(CacheKeys.GOODS_DETAIL + order.getGoodsId());

        notificationService.send(order.getSellerId(), "ORDER_CANCELLED", "买家已取消订单",
                "订单 " + order.getOrderNo() + " 已被买家取消", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 模拟支付：0 待付款 -> 1 已付款，CAS 保证重复点击只会成功一次
        int rows = orderMapper.updateStatus(orderId,
                StatusConstant.ORDER_PENDING_PAYMENT, StatusConstant.ORDER_PAID);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_PAY);
        }

        notificationService.send(order.getSellerId(), "ORDER_PAID", "买家已付款",
                "订单 " + order.getOrderNo() + " 已支付 ¥" + order.getAmount() + "，请尽快发货", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 退款处理中不允许发货
        if (order.getRefundStatus() != null && order.getRefundStatus() == 1) {
            throw new BusinessException(ErrorCode.ORDER_REFUND_PROCESSING);
        }

        // 1 已付款 -> 2 已发货
        int rows = orderMapper.updateStatus(orderId,
                StatusConstant.ORDER_PAID, StatusConstant.ORDER_SHIPPED);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_SHIP);
        }

        notificationService.send(order.getBuyerId(), "ORDER_SHIPPED", "卖家已发货",
                "订单 " + order.getOrderNo() + " 已发货，请注意查收并确认收货", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 退款处理中不允许确认收货
        if (order.getRefundStatus() != null && order.getRefundStatus() == 1) {
            throw new BusinessException(ErrorCode.ORDER_REFUND_PROCESSING);
        }

        // 允许 已发货(2) -> 已收货(3)；也兼容卖家未点发货时买家直接确认（已付款(1) -> 已收货(3)）
        int rows = orderMapper.updateStatus(orderId,
                StatusConstant.ORDER_SHIPPED, StatusConstant.ORDER_RECEIVED);
        if (rows == 0) {
            rows = orderMapper.updateStatus(orderId,
                    StatusConstant.ORDER_PAID, StatusConstant.ORDER_RECEIVED);
        }
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CONFIRM);
        }

        notificationService.send(order.getSellerId(), "ORDER_RECEIVED", "买家已确认收货",
                "订单 " + order.getOrderNo() + " 交易完成，可以互相评价了", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        int rows = orderMapper.applyRefund(orderId, reason);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_REFUND);
        }

        notificationService.send(order.getSellerId(), "REFUND_APPLY", "买家申请退款",
                "订单 " + order.getOrderNo() + " 买家申请退款" + (reason == null || reason.isBlank() ? "" : "，原因：" + reason)
                        + "，请及时处理", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agreeRefund(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        int rows = orderMapper.agreeRefund(orderId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_AGREE_REFUND);
        }

        // 退款完成：商品恢复在售
        goodsMapper.restoreOnSale(order.getGoodsId());
        cacheService.evict(CacheKeys.GOODS_DETAIL + order.getGoodsId());

        notificationService.send(order.getBuyerId(), "REFUND_AGREED", "退款已完成",
                "订单 " + order.getOrderNo() + " 的退款申请已通过，¥" + order.getAmount() + " 将原路退回（模拟）", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        int rows = orderMapper.rejectRefund(orderId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_REJECT_REFUND);
        }

        notificationService.send(order.getBuyerId(), "REFUND_REJECTED", "退款申请被拒绝",
                "订单 " + order.getOrderNo() + " 的退款申请被卖家拒绝，如有疑问可联系卖家协商", orderId);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    /**
     * 生成订单编号
     * 格式: CT + yyyyMMdd + 6位序号
     */
    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = ORDER_COUNTER.incrementAndGet() % 1000000;
        return String.format("CT%s%06d", datePart, sequence);
    }
}