package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.constant.TradeRecordType;
import com.campustrade.dto.WalletDelta;
import com.campustrade.entity.Order;
import com.campustrade.entity.TradeRecord;
import com.campustrade.entity.Wallet;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.TradeRecordMapper;
import com.campustrade.mapper.WalletMapper;
import com.campustrade.service.WalletService;
import com.campustrade.vo.WalletDiscrepancy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 钱包与担保交易实现。
 * <p>
 * 幂等设计（支付回调可能重复投递）：
 * 1. 先按 (order_id, type, user_id) 查流水，已存在直接返回；
 * 2. 数据库层再加 uk_trade_no 与 uk_order_type_user 唯一索引兜底，
 *    并发穿透时后者会抛 DuplicateKeyException，整个事务回滚，不会重复扣款。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final TradeRecordMapper tradeRecordMapper;

    @Override
    public Wallet getWallet(Long userId) {
        Wallet wallet = selectByUserId(userId);
        return wallet != null ? wallet : createWallet(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        requireValidAmount(amount);
        getWallet(userId);
        walletMapper.credit(userId, amount);
        Wallet after = getWallet(userId);
        insertRecord(null, userId, TradeRecordType.RECHARGE, amount, after, "账户充值");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeForPayment(Order order) {
        if (order == null || order.getId() == null || order.getAmount() == null) {
            return;
        }
        Long buyerId = order.getBuyerId();
        // 幂等：同一订单只冻结一次，防止支付接口被重复调用
        if (tradeRecordMapper.selectByOrderAndType(order.getId(), buyerId, TradeRecordType.PAY.getCode()) != null) {
            log.info("订单 {} 已冻结过资金，忽略重复支付请求", order.getOrderNo());
            return;
        }

        getWallet(buyerId);
        // 原子冻结：可用余额不足时影响行数为 0，不会出现负余额
        if (walletMapper.freeze(buyerId, order.getAmount()) == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        Wallet after = getWallet(buyerId);
        insertRecord(order.getId(), buyerId, TradeRecordType.PAY, order.getAmount(), after,
                "订单支付冻结：" + order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settle(Order order) {
        if (order == null || order.getId() == null || order.getAmount() == null) {
            return;
        }
        // 幂等：买家结算转出流水已存在说明已结算过
        if (tradeRecordMapper.selectByOrderAndType(order.getId(), order.getBuyerId(),
                TradeRecordType.SETTLE_OUT.getCode()) != null) {
            log.info("订单 {} 已结算，忽略重复结算请求", order.getOrderNo());
            return;
        }

        getWallet(order.getBuyerId());
        // 钱包上线前的历史订单没有冻结资金：不入账，避免凭空给卖家加钱
        if (walletMapper.deductFrozen(order.getBuyerId(), order.getAmount()) == 0) {
            log.warn("订单 {} 无冻结资金，跳过结算（可能是钱包上线前的历史订单）", order.getOrderNo());
            return;
        }
        Wallet buyerAfter = getWallet(order.getBuyerId());
        insertRecord(order.getId(), order.getBuyerId(), TradeRecordType.SETTLE_OUT, order.getAmount(),
                buyerAfter, "确认收货结算：" + order.getOrderNo());

        getWallet(order.getSellerId());
        walletMapper.credit(order.getSellerId(), order.getAmount());
        Wallet sellerAfter = getWallet(order.getSellerId());
        insertRecord(order.getId(), order.getSellerId(), TradeRecordType.SETTLE_IN, order.getAmount(),
                sellerAfter, "订单结算入账：" + order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Order order) {
        if (order == null || order.getId() == null || order.getAmount() == null) {
            return;
        }
        if (tradeRecordMapper.selectByOrderAndType(order.getId(), order.getBuyerId(),
                TradeRecordType.REFUND.getCode()) != null) {
            log.info("订单 {} 已退款，忽略重复退款请求", order.getOrderNo());
            return;
        }

        getWallet(order.getBuyerId());
        if (walletMapper.releaseFrozen(order.getBuyerId(), order.getAmount()) == 0) {
            log.warn("订单 {} 无冻结资金，跳过退款（可能是钱包上线前的历史订单）", order.getOrderNo());
            return;
        }
        Wallet after = getWallet(order.getBuyerId());
        insertRecord(order.getId(), order.getBuyerId(), TradeRecordType.REFUND, order.getAmount(), after,
                "退款解冻：" + order.getOrderNo());
    }

    @Override
    public IPage<TradeRecord> pageRecords(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<TradeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TradeRecord::getUserId, userId).orderByDesc(TradeRecord::getId);
        return tradeRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<WalletDiscrepancy> reconcile() {
        List<Wallet> wallets = walletMapper.selectList(null);
        if (wallets == null || wallets.isEmpty()) {
            return Collections.emptyList();
        }
        List<WalletDelta> deltas = tradeRecordMapper.sumDeltasByUser();
        Map<Long, WalletDelta> deltaMap = new HashMap<>();
        if (deltas != null) {
            for (WalletDelta delta : deltas) {
                deltaMap.put(delta.getUserId(), delta);
            }
        }

        List<WalletDiscrepancy> result = new ArrayList<>();
        for (Wallet wallet : wallets) {
            WalletDelta delta = deltaMap.get(wallet.getUserId());
            BigDecimal expectedAvailable = delta == null || delta.getAvailableDelta() == null
                    ? BigDecimal.ZERO : delta.getAvailableDelta();
            BigDecimal expectedFrozen = delta == null || delta.getFrozenDelta() == null
                    ? BigDecimal.ZERO : delta.getFrozenDelta();

            boolean availableOk = equalsAmount(wallet.getAvailableAmount(), expectedAvailable);
            boolean frozenOk = equalsAmount(wallet.getFrozenAmount(), expectedFrozen);
            if (!availableOk || !frozenOk) {
                result.add(new WalletDiscrepancy(wallet.getUserId(), wallet.getAvailableAmount(),
                        expectedAvailable, wallet.getFrozenAmount(), expectedFrozen));
            }
        }
        return result;
    }

    private Wallet selectByUserId(Long userId) {
        return walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
    }

    /**
     * 创建零余额钱包；并发首充时可能同时创建，撞唯一索引后回查即可。
     */
    private Wallet createWallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAvailableAmount(BigDecimal.ZERO);
        wallet.setFrozenAmount(BigDecimal.ZERO);
        try {
            walletMapper.insert(wallet);
            return wallet;
        } catch (DuplicateKeyException e) {
            Wallet existing = selectByUserId(userId);
            if (existing == null) {
                throw e;
            }
            return existing;
        }
    }

    private void insertRecord(Long orderId, Long userId, TradeRecordType type, BigDecimal amount,
                              Wallet after, String remark) {
        TradeRecord record = new TradeRecord();
        record.setTradeNo("TR" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setType(type.getCode());
        record.setAmount(amount);
        record.setAvailableAfter(after.getAvailableAmount());
        record.setFrozenAfter(after.getFrozenAmount());
        record.setRemark(remark);
        tradeRecordMapper.insert(record);
    }

    private void requireValidAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID);
        }
        // 两位小数保护：超过 2 位直接拒绝，避免四舍五入造成对账尾差
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessException(ErrorCode.WALLET_AMOUNT_INVALID);
        }
    }

    private boolean equalsAmount(BigDecimal a, BigDecimal b) {
        return Objects.requireNonNullElse(a, BigDecimal.ZERO).compareTo(Objects.requireNonNullElse(b, BigDecimal.ZERO)) == 0;
    }
}