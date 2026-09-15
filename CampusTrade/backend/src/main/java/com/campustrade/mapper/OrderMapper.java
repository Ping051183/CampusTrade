package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询买家订单
     */
    IPage<Order> selectByBuyerId(Page<Order> page, @Param("buyerId") Long buyerId,
                                  @Param("status") Integer status);

    /**
     * 分页查询卖家订单
     */
    IPage<Order> selectBySellerId(Page<Order> page, @Param("sellerId") Long sellerId,
                                   @Param("status") Integer status);

    /**
     * 统计今日订单数
     */
    Long countToday();

    /**
     * 统计今日交易额
     */
    BigDecimal sumTodayAmount();

    /**
     * 统计总订单数
     */
    Long countTotal();

    /**
     * 统计总交易额
     */
    BigDecimal sumTotalAmount();
}
