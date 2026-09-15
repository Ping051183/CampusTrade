package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.Goods;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏Controller
 */
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@Tag(name = "收藏管理", description = "商品收藏接口")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 添加收藏
     */
    @PostMapping("/add/{goodsId}")
    @Operation(summary = "添加收藏")
    public Result<Void> addFavorite(@PathVariable Long goodsId) {
        Long userId = SecurityUtil.getCurrentUserId();
        favoriteService.addFavorite(userId, goodsId);
        return Result.success("收藏成功", null);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/cancel/{goodsId}")
    @Operation(summary = "取消收藏")
    public Result<Void> cancelFavorite(@PathVariable Long goodsId) {
        Long userId = SecurityUtil.getCurrentUserId();
        favoriteService.cancelFavorite(userId, goodsId);
        return Result.success("已取消收藏", null);
    }

    /**
     * 获取我的收藏列表
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的收藏列表")
    public Result<List<Goods>> getMyFavorites() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(favoriteService.getUserFavorites(userId));
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check/{goodsId}")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> checkFavorited(@PathVariable Long goodsId) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(favoriteService.isFavorited(userId, goodsId));
    }
}
