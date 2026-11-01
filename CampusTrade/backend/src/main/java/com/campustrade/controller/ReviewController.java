package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.ReviewDTO;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.ReviewService;
import com.campustrade.vo.ReviewSummaryVO;
import com.campustrade.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 交易评价Controller
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Tag(name = "交易评价", description = "订单评价与用户信用查询接口")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 对已完成订单发表评价（买家评价卖家 / 卖家评价买家）
     */
    @PostMapping
    @Operation(summary = "发表评价")
    public Result<Void> createReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        Long userId = SecurityUtil.getCurrentUserId();
        reviewService.createReview(reviewDTO.getOrderId(), userId, reviewDTO.getScore(), reviewDTO.getContent());
        return Result.success("评价成功", null);
    }

    /**
     * 查询某个用户收到的评价（公开）
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户评价列表")
    public Result<PageResult<ReviewVO>> pageByUser(@PathVariable Long userId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reviewService.pageByRateeId(userId, page, pageSize));
    }

    /**
     * 查询某个用户的信用汇总（公开）
     */
    @GetMapping("/summary/{userId}")
    @Operation(summary = "查询用户信用汇总")
    public Result<ReviewSummaryVO> summary(@PathVariable Long userId) {
        return Result.success(reviewService.summary(userId));
    }
}