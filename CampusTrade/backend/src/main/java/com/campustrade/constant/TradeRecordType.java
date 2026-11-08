package com.campustrade.constant;

/**
 * 资金流水类型。
 * <p>
 * 每种类型对钱包的影响：
 * 充值   RECHARGE   可用 +
 * 支付   PAY        可用 -，冻结 +
 * 退款   REFUND     可用 +，冻结 -
 * 结算转出 SETTLE_OUT 冻结 -
 * 结算入账 SETTLE_IN  可用 +
 */
public enum TradeRecordType {

    RECHARGE(1, "充值"),
    PAY(2, "支付冻结"),
    REFUND(3, "退款解冻"),
    SETTLE_OUT(4, "结算转出"),
    SETTLE_IN(5, "结算入账");

    private final int code;
    private final String desc;

    TradeRecordType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}