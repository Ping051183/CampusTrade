package com.campustrade.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 对账差异：钱包余额与流水净额不一致的账户。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletDiscrepancy {

    private Long userId;

    /** 钱包里的可用余额 */
    private BigDecimal available;

    /** 流水推算出的可用余额 */
    private BigDecimal expectedAvailable;

    /** 钱包里的冻结余额 */
    private BigDecimal frozen;

    /** 流水推算出的冻结余额 */
    private BigDecimal expectedFrozen;
}