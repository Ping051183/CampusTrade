package com.campustrade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campustrade.entity.Order;
import com.campustrade.entity.TradeRecord;
import com.campustrade.entity.Wallet;
import com.campustrade.vo.WalletDiscrepancy;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包与担保交易服务。
 * <p>
 * 资金流转：充值 → 支付冻结（买家可用转冻结）→ 确认收货结算（买家冻结扣减、卖家可用增加）
 * 或 退款解冻（买家冻结转回可用）。
 */
public interface WalletService {

    /** 查询钱包，不存在时创建零余额钱包 */
    Wallet getWallet(Long userId);

    /** 充值（演示用，真实场景对接支付网关回调） */
    void recharge(Long userId, BigDecimal amount);

    /** 订单支付时冻结买家资金；余额不足抛 INSUFFICIENT_BALANCE */
    void freezeForPayment(Order order);

    /** 确认收货后结算：买家冻结扣减，卖家可用增加（幂等） */
    void settle(Order order);

    /** 退款：买家冻结转回可用（幂等） */
    void refund(Order order);

    /** 我的资金流水 */
    IPage<TradeRecord> pageRecords(Long userId, int page, int pageSize);

    /** 全量对账：钱包余额 vs 流水净额，返回不平的账户 */
    List<WalletDiscrepancy> reconcile();
}