package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Order;

import java.util.List;

/**
 * 订单Service接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     */
    Order createOrder(Long buyerId, Long goodsId, String remark);

    /**
     * 获取订单详情
     */
    Order getOrderDetail(Long orderId, Long userId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * 支付订单（模拟支付：待付款 -> 已付款）
     */
    void payOrder(Long orderId, Long userId);

    /**
     * 卖家发货（已付款 -> 已发货）
     */
    void shipOrder(Long orderId, Long userId);

    /**
     * 确认收货（已付款/已发货 -> 已收货）
     */
    void confirmReceipt(Long orderId, Long userId);

    /**
     * 买家申请退款（已付款/已发货 -> 退款申请中）
     */
    void applyRefund(Long orderId, Long userId, String reason);

    /**
     * 卖家同意退款（退款申请中 -> 已退款，订单取消、商品恢复在售）
     */
    void agreeRefund(Long orderId, Long userId);

    /**
     * 卖家拒绝退款（退款申请中 -> 已拒绝）
     */
    void rejectRefund(Long orderId, Long userId);

    /**
     * 根据订单编号查询订单
     */
    Order getByOrderNo(String orderNo);

    /**
     * 查询超时未支付的订单ID（待付款且创建时间早于 timeoutMinutes 分钟前）
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @param limit          单批最大数量
     */
    List<Long> findTimeoutUnpaidOrderIds(int timeoutMinutes, int limit);

    /**
     * 取消单个超时未支付订单（独立事务，CAS 0 -> 4，商品恢复在售并通知双方）
     *
     * @return true 表示本次确实取消成功，false 表示订单已被支付/取消
     */
    boolean cancelByTimeout(Long orderId, int timeoutMinutes);
}
