package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campustrade.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 钱包Mapper。
 * <p>
 * 所有金额变更都用「带条件的原子 UPDATE」：条件不满足时影响行数为 0，
 * 由调用方抛业务异常，避免 SELECT 再 UPDATE 之间被并发请求插入。
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    /**
     * 冻结：可用余额 -> 冻结余额，余额不足时返回 0。
     */
    @Update("UPDATE wallet SET available_amount = available_amount - #{amount}, "
            + "frozen_amount = frozen_amount + #{amount} "
            + "WHERE user_id = #{userId} AND available_amount >= #{amount}")
    int freeze(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 解冻：冻结余额 -> 可用余额（退款场景），冻结不足时返回 0。
     */
    @Update("UPDATE wallet SET frozen_amount = frozen_amount - #{amount}, "
            + "available_amount = available_amount + #{amount} "
            + "WHERE user_id = #{userId} AND frozen_amount >= #{amount}")
    int releaseFrozen(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 冻结余额直接扣减（结算给卖家场景），冻结不足时返回 0。
     */
    @Update("UPDATE wallet SET frozen_amount = frozen_amount - #{amount} "
            + "WHERE user_id = #{userId} AND frozen_amount >= #{amount}")
    int deductFrozen(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 可用余额增加（充值 / 卖家结算入账）。
     */
    @Update("UPDATE wallet SET available_amount = available_amount + #{amount} WHERE user_id = #{userId}")
    int credit(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}