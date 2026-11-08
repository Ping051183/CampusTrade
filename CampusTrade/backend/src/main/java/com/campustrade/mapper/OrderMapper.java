package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 条件更新订单状态（CAS）：只有当前状态等于 fromStatus 时才更新为 toStatus
     * 用于支付、发货、确认收货、取消等状态流转，避免并发重复操作
     *
     * @return 受影响行数，0 表示状态已被其他请求改变
     */
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);

    /**
     * 买家申请退款：仅 已付款(1)/已发货(2) 且尚未申请过退款的订单可以申请
     *
     * @return 受影响行数
     */
    int applyRefund(@Param("id") Long id, @Param("reason") String reason);

    /**
     * 卖家同意退款：退款状态 1 -> 2，同时订单状态置为已取消(4)
     *
     * @return 受影响行数
     */
    int agreeRefund(@Param("id") Long id);

    /**
     * 卖家拒绝退款：退款状态 1 -> 3
     *
     * @return 受影响行数
     */
    int rejectRefund(@Param("id") Long id);

    /**
     * CAS 标记买家已评价
     */
    int markBuyerRated(@Param("id") Long id);

    /**
     * CAS 标记卖家已评价
     */
    int markSellerRated(@Param("id") Long id);

    /**
     * 查询超时未支付的订单ID（定时任务自动取消用）
     *
     * @param deadline 创建时间早于该时间点的订单视为超时
     * @param limit    单批最大处理数量
     */
    List<Long> selectTimeoutUnpaidIds(@Param("deadline") LocalDateTime deadline,
                                      @Param("limit") int limit);
}
