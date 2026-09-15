package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Order;

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
     * 确认收货
     */
    void confirmReceipt(Long orderId, Long userId);

    /**
     * 根据订单编号查询订单
     */
    Order getByOrderNo(String orderNo);
}
