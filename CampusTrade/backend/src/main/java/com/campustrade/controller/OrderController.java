package com.campustrade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.Order;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单Controller
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单创建、查询等接口")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    /**
     * 创建订单（购买商品）
     */
    @PostMapping("/create/{goodsId}")
    @Operation(summary = "创建订单")
    public Result<Order> createOrder(@PathVariable Long goodsId,
                                      @RequestParam(required = false) String remark) {
        Long userId = SecurityUtil.getCurrentUserId();
        Order order = orderService.createOrder(userId, goodsId, remark);
        return Result.success("下单成功", order);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取订单详情")
    public Result<Order> getOrderDetail(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(orderService.getOrderDetail(id, userId));
    }

    /**
     * 获取我的订单（买家视角）
     */
    @GetMapping("/my-buy")
    @Operation(summary = "获取我的购买订单")
    public Result<PageResult<Order>> getMyBuyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<Order> pageParam = new Page<>(page, pageSize);
        return Result.success(PageResult.from(orderMapper.selectByBuyerId(pageParam, userId, status)));
    }

    /**
     * 获取我的订单（卖家视角）
     */
    @GetMapping("/my-sell")
    @Operation(summary = "获取我的出售订单")
    public Result<PageResult<Order>> getMySellOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<Order> pageParam = new Page<>(page, pageSize);
        return Result.success(PageResult.from(orderMapper.selectBySellerId(pageParam, userId, status)));
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        orderService.cancelOrder(id, userId);
        return Result.success("订单已取消", null);
    }

    /**
     * 确认收货
     */
    @PutMapping("/confirm/{id}")
    @Operation(summary = "确认收货")
    public Result<Void> confirmReceipt(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        orderService.confirmReceipt(id, userId);
        return Result.success("已确认收货", null);
    }
}
