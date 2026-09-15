package com.campustrade.constant;

/**
 * 状态常量类
 */
public interface StatusConstant {

    // ========== 用户状态 ==========
    /** 用户-禁用 */
    Integer USER_DISABLED = 0;
    /** 用户-启用 */
    Integer USER_ENABLED = 1;

    // ========== 商品状态 ==========
    /** 商品-待审核 */
    Integer GOODS_PENDING = 0;
    /** 商品-在售 */
    Integer GOODS_ON_SALE = 1;
    /** 商品-已下架 */
    Integer GOODS_OFFLINE = 2;
    /** 商品-已售出 */
    Integer GOODS_SOLD = 3;
    /** 商品-审核驳回 */
    Integer GOODS_REJECTED = 4;

    // ========== 订单状态 ==========
    /** 订单-待付款 */
    Integer ORDER_PENDING_PAYMENT = 0;
    /** 订单-已付款 */
    Integer ORDER_PAID = 1;
    /** 订单-已发货 */
    Integer ORDER_SHIPPED = 2;
    /** 订单-已收货 */
    Integer ORDER_RECEIVED = 3;
    /** 订单-已取消 */
    Integer ORDER_CANCELLED = 4;
    /** 订单-已完成 */
    Integer ORDER_COMPLETED = 5;

    // ========== 举报状态 ==========
    /** 举报-待处理 */
    Integer REPORT_PENDING = 0;
    /** 举报-已处理 */
    Integer REPORT_HANDLED = 1;
    /** 举报-驳回 */
    Integer REPORT_REJECTED = 2;
}
