package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.service.WalletService;
import com.campustrade.vo.WalletDiscrepancy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台资金对账接口（仅 ADMIN，由 SecurityConfig 的 /api/admin/** 规则保护）。
 */
@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
@Tag(name = "后台-资金对账", description = "钱包余额与流水核对")
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping("/reconcile")
    @Operation(summary = "全量对账，返回余额与流水不一致的账户")
    public Result<List<WalletDiscrepancy>> reconcile() {
        List<WalletDiscrepancy> discrepancies = walletService.reconcile();
        return Result.success("对账完成，不平账户 " + discrepancies.size() + " 个", discrepancies);
    }
}