package com.campustrade.controller;

import com.campustrade.annotation.RateLimit;
import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.RechargeRequest;
import com.campustrade.entity.TradeRecord;
import com.campustrade.entity.Wallet;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.WalletService;
import com.campustrade.vo.WalletVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 钱包Controller：余额、流水、充值。
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "钱包管理", description = "余额查询、资金流水、充值")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "查询我的钱包余额")
    public Result<WalletVO> getWallet() {
        Wallet wallet = walletService.getWallet(SecurityUtil.getCurrentUserId());
        return Result.success(new WalletVO(wallet.getAvailableAmount(), wallet.getFrozenAmount(), wallet.total()));
    }

    @GetMapping("/records")
    @Operation(summary = "查询我的资金流水")
    public Result<PageResult<TradeRecord>> getRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(PageResult.from(walletService.pageRecords(userId, page, pageSize)));
    }

    @PostMapping("/recharge")
    @RateLimit(limit = 5, windowSeconds = 60, key = "wallet-recharge")
    @Operation(summary = "充值（演示用，真实场景走支付网关回调）")
    public Result<WalletVO> recharge(@Valid @RequestBody RechargeRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        walletService.recharge(userId, request.getAmount());
        Wallet wallet = walletService.getWallet(userId);
        return Result.success("充值成功", new WalletVO(wallet.getAvailableAmount(), wallet.getFrozenAmount(), wallet.total()));
    }
}