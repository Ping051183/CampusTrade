package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户钱包：可用余额 + 冻结余额（担保交易中已支付但未确认收货的资金）。
 */
@Data
@TableName("wallet")
public class Wallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，唯一 */
    private Long userId;

    /** 可用余额 */
    private BigDecimal availableAmount;

    /** 冻结余额（买家已付款、尚未结算给卖家的托管资金） */
    private BigDecimal frozenAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 可用 + 冻结 */
    public BigDecimal total() {
        BigDecimal available = availableAmount == null ? BigDecimal.ZERO : availableAmount;
        BigDecimal frozen = frozenAmount == null ? BigDecimal.ZERO : frozenAmount;
        return available.add(frozen);
    }
}