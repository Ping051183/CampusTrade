package com.campustrade.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 对账中间结果：某用户流水的净影响。
 */
@Data
public class WalletDelta {

    private Long userId;

    /** 可用余额净变化 */
    private BigDecimal availableDelta;

    /** 冻结余额净变化 */
    private BigDecimal frozenDelta;
}