package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.Notice;
import com.campustrade.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告Controller
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
@Tag(name = "公告管理", description = "系统公告接口")
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 获取最新公告列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取最新公告")
    public Result<List<Notice>> getLatestNotices() {
        return Result.success(noticeService.getLatestNotices(10));
    }
}
