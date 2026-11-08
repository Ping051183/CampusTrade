package com.campustrade.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 钱包概览。
 */
@Data
@AllArgsConstructor
public class WalletVO {

    /** 可用余额 */
    private BigDecimal availableAmount;

    /** 冻结余额（担保交易中在途资金） */
    private BigDecimal frozenAmount;

    /** 总余额 */
    private BigDecimal totalAmount;
}