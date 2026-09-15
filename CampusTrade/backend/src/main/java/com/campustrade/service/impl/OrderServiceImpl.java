package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.Goods;
import com.campustrade.entity.Order;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单Service实现类
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;

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

        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getSellerId());
        order.setGoodsId(goodsId);
        order.setAmount(goods.getPrice());
        order.setStatus(StatusConstant.ORDER_PENDING_PAYMENT);
        order.setRemark(remark);
        orderMapper.insert(order);

        // 更新商品状态为已售出
        goods.setStatus(StatusConstant.GOODS_SOLD);
        goodsMapper.updateById(goods);

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
        if (!StatusConstant.ORDER_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // 取消订单
        order.setStatus(StatusConstant.ORDER_CANCELLED);
        orderMapper.updateById(order);

        // 恢复商品为在售状态
        Goods goods = goodsMapper.selectById(order.getGoodsId());
        if (goods != null) {
            goods.setStatus(StatusConstant.GOODS_ON_SALE);
            goodsMapper.updateById(goods);
        }
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
        // 简化流程：从"已付款"直接到"已收货"
        if (!StatusConstant.ORDER_PAID.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CONFIRM);
        }

        order.setStatus(StatusConstant.ORDER_RECEIVED);
        orderMapper.updateById(order);
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
