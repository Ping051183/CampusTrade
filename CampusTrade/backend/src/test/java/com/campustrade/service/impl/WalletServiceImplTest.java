package com.campustrade.service.impl;

import com.campustrade.entity.Order;
import com.campustrade.entity.TradeRecord;
import com.campustrade.entity.Wallet;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.TradeRecordMapper;
import com.campustrade.mapper.WalletMapper;
import com.campustrade.vo.WalletDiscrepancy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletServiceImpl 单元测试：冻结、结算、退款、幂等与对账。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("钱包与担保交易")
class WalletServiceImplTest {

    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 3L;
    private static final Long ORDER_ID = 900L;
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");

    @Mock
    private WalletMapper walletMapper;
    @Mock
    private TradeRecordMapper tradeRecordMapper;

    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(walletMapper, tradeRecordMapper);
    }

    private Wallet wallet(Long userId, String available, String frozen) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAvailableAmount(new BigDecimal(available));
        wallet.setFrozenAmount(new BigDecimal(frozen));
        return wallet;
    }

    private Order order() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("CT20240101000001");
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setAmount(AMOUNT);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    @Test
    @DisplayName("余额不足时支付失败，不写流水")
    void freezeFailsWhenBalanceNotEnough() {
        when(walletMapper.selectOne(any())).thenReturn(wallet(BUYER_ID, "10.00", "0.00"));
        when(tradeRecordMapper.selectByOrderAndType(eq(ORDER_ID), eq(BUYER_ID), eq(2))).thenReturn(null);
        when(walletMapper.freeze(eq(BUYER_ID), eq(AMOUNT))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> walletService.freezeForPayment(order()));

        assertEquals(ErrorCode.INSUFFICIENT_BALANCE.getCode(), ex.getCode());
        verify(tradeRecordMapper, never()).insert(any(TradeRecord.class));
    }

    @Test
    @DisplayName("支付成功后写一条支付冻结流水")
    void freezeWritesTradeRecord() {
        when(walletMapper.selectOne(any())).thenReturn(wallet(BUYER_ID, "200.00", "0.00"));
        when(tradeRecordMapper.selectByOrderAndType(eq(ORDER_ID), eq(BUYER_ID), eq(2))).thenReturn(null);
        when(walletMapper.freeze(eq(BUYER_ID), eq(AMOUNT))).thenReturn(1);

        walletService.freezeForPayment(order());

        ArgumentCaptor<TradeRecord> captor = ArgumentCaptor.forClass(TradeRecord.class);
        verify(tradeRecordMapper).insert(captor.capture());
        TradeRecord record = captor.getValue();
        assertEquals(2, record.getType());
        assertEquals(AMOUNT, record.getAmount());
        assertEquals(ORDER_ID, record.getOrderId());
        assertTrue(record.getTradeNo().startsWith("TR"));
    }

    @Test
    @DisplayName("重复支付请求命中已有流水时不再冻结")
    void freezeIsIdempotent() {
        when(tradeRecordMapper.selectByOrderAndType(eq(ORDER_ID), eq(BUYER_ID), eq(2)))
                .thenReturn(new TradeRecord());

        walletService.freezeForPayment(order());

        verify(walletMapper, never()).freeze(anyLong(), any(BigDecimal.class));
        verify(tradeRecordMapper, never()).insert(any(TradeRecord.class));
    }

    @Test
    @DisplayName("确认收货结算：买家冻结扣减，卖家可用增加，写两条流水")
    void settleMovesFrozenToSeller() {
        when(tradeRecordMapper.selectByOrderAndType(eq(ORDER_ID), eq(BUYER_ID), eq(4))).thenReturn(null);
        when(walletMapper.selectOne(any())).thenReturn(wallet(BUYER_ID, "50.00", "150.00"));
        when(walletMapper.deductFrozen(eq(BUYER_ID), eq(AMOUNT))).thenReturn(1);

        walletService.settle(order());

        verify(walletMapper).deductFrozen(BUYER_ID, AMOUNT);
        verify(walletMapper).credit(SELLER_ID, AMOUNT);
        verify(tradeRecordMapper, times(2)).insert(any(TradeRecord.class));
    }

    @Test
    @DisplayName("重复结算命中已有流水时不再打款")
    void settleIsIdempotent() {
        when(tradeRecordMapper.selectByOrderAndType(eq(ORDER_ID), eq(BUYER_ID), eq(4)))
                .thenReturn(new TradeRecord());

        walletService.settle(order());

        verify(walletMapper, never()).deductFrozen(anyLong(), any(BigDecimal.class));
        verify(walletMapper, never()).credit(anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("历史订单没有冻结资金时跳过结算，不给卖家凭空加钱")
    void settleSkipsLegacyOrder() {
        when(tradeRecordMapper.selectByOrderAndType(eq(ORDER_ID), eq(BUYER_ID), eq(4))).thenReturn(null);
        when(walletMapper.selectOne(any())).thenReturn(wallet(BUYER_ID, "0.00", "0.00"));
        when(walletMapper.deductFrozen(eq(BUYER_ID), eq(AMOUNT))).thenReturn(0);

        walletService.settle(order());

        verify(walletMapper, never()).credit(anyLong(), any(BigDecimal.class));
        verify(tradeRecordMapper, never()).insert(any(TradeRecord.class));
    }

    @Test
    @DisplayName("退款把冻结余额解冻回可用余额")
    void refundReleasesFrozen() {
        when(tradeRecordMapper.selectByOrderAndType(eq(ORDER_ID), eq(BUYER_ID), eq(3))).thenReturn(null);
        when(walletMapper.selectOne(any())).thenReturn(wallet(BUYER_ID, "0.00", "150.00"));
        when(walletMapper.releaseFrozen(eq(BUYER_ID), eq(AMOUNT))).thenReturn(1);

        walletService.refund(order());

        verify(walletMapper).releaseFrozen(BUYER_ID, AMOUNT);
        ArgumentCaptor<TradeRecord> captor = ArgumentCaptor.forClass(TradeRecord.class);
        verify(tradeRecordMapper).insert(captor.capture());
        assertEquals(3, captor.getValue().getType());
    }

    @Test
    @DisplayName("对账能发现余额与流水不一致的账户")
    void reconcileFindsDiscrepancy() {
        when(walletMapper.selectList(null)).thenReturn(List.of(wallet(1L, "100.00", "0.00")));
        com.campustrade.dto.WalletDelta delta = new com.campustrade.dto.WalletDelta();
        delta.setUserId(1L);
        delta.setAvailableDelta(new BigDecimal("60.00"));
        delta.setFrozenDelta(BigDecimal.ZERO);
        when(tradeRecordMapper.sumDeltasByUser()).thenReturn(List.of(delta));

        List<WalletDiscrepancy> result = walletService.reconcile();

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).getAvailable());
        assertEquals(new BigDecimal("60.00"), result.get(0).getExpectedAvailable());
    }

    @Test
    @DisplayName("对账一致时返回空列表")
    void reconcilePassesWhenBalanced() {
        when(walletMapper.selectList(null)).thenReturn(List.of(wallet(1L, "100.00", "20.00")));
        com.campustrade.dto.WalletDelta delta = new com.campustrade.dto.WalletDelta();
        delta.setUserId(1L);
        delta.setAvailableDelta(new BigDecimal("100.00"));
        delta.setFrozenDelta(new BigDecimal("20.00"));
        when(tradeRecordMapper.sumDeltasByUser()).thenReturn(List.of(delta));

        assertTrue(walletService.reconcile().isEmpty());
    }
}