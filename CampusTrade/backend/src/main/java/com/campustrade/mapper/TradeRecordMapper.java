package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campustrade.dto.WalletDelta;
import com.campustrade.entity.TradeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资金流水Mapper。
 */
@Mapper
public interface TradeRecordMapper extends BaseMapper<TradeRecord> {

    /**
     * 按订单 + 用户 + 类型查流水，用于幂等判断（重复回调时不再扣款/入账）。
     */
    @Select("SELECT * FROM trade_record WHERE order_id = #{orderId} AND user_id = #{userId} "
            + "AND type = #{type} LIMIT 1")
    TradeRecord selectByOrderAndType(@Param("orderId") Long orderId,
                                     @Param("userId") Long userId,
                                     @Param("type") Integer type);

    /**
     * 对账用：按用户汇总流水对余额的净影响。
     */
    @Select("SELECT user_id AS userId, "
            + "COALESCE(SUM(CASE type WHEN 1 THEN amount WHEN 2 THEN -amount WHEN 3 THEN amount "
            + "WHEN 5 THEN amount ELSE 0 END), 0) AS availableDelta, "
            + "COALESCE(SUM(CASE type WHEN 2 THEN amount WHEN 3 THEN -amount WHEN 4 THEN -amount "
            + "ELSE 0 END), 0) AS frozenDelta "
            + "FROM trade_record GROUP BY user_id")
    List<WalletDelta> sumDeltasByUser();
}