package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.Report;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 举报Controller
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(name = "举报管理", description = "商品举报接口")
public class ReportController {

    private final ReportService reportService;

    /**
     * 举报商品
     */
    @PostMapping("/add")
    @Operation(summary = "举报商品")
    public Result<Void> addReport(@RequestBody Report report) {
        Long userId = SecurityUtil.getCurrentUserId();
        report.setReporterId(userId);
        reportService.addReport(report);
        return Result.success("举报成功，等待处理", null);
    }
}
