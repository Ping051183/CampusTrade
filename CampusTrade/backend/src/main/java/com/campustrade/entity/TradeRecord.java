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
 * 资金流水，担保交易的唯一账本。
 * <p>
 * 幂等：uk_trade_no 唯一索引 + (order_id, type, user_id) 唯一索引。
 * 支付回调重复到达时，第二次插入会撞唯一键，从而不会重复扣款/入账。
 */
@Data
@TableName("trade_record")
public class TradeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流水号，全局唯一 */
    private String tradeNo;

    /** 关联订单ID，充值等非订单流水为空 */
    private Long orderId;

    /** 资金所属用户 */
    private Long userId;

    /** 流水类型：1-充值 2-支付冻结 3-退款解冻 4-结算转出（买家） 5-结算入账（卖家） */
    private Integer type;

    /** 发生金额（正数，方向由 type 决定） */
    private BigDecimal amount;

    /** 交易后可用余额快照 */
    private BigDecimal availableAfter;

    /** 交易后冻结余额快照 */
    private BigDecimal frozenAfter;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}