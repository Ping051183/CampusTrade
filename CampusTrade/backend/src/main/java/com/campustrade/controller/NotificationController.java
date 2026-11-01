package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.Notification;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知Controller
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Tag(name = "系统通知", description = "订单状态变更等系统通知接口")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 我的通知列表
     */
    @GetMapping("/list")
    @Operation(summary = "我的通知列表")
    public Result<PageResult<Notification>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(notificationService.page(userId, page, pageSize, unreadOnly));
    }

    /**
     * 未读通知数
     */
    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(notificationService.unreadCount(userId));
    }

    /**
     * 标记单条已读
     */
    @PutMapping("/read/{id}")
    @Operation(summary = "标记通知已读")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markRead(id, userId);
        return Result.success();
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAllRead(userId);
        return Result.success();
    }
}