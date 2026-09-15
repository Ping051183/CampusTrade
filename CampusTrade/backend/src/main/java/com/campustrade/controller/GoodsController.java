package com.campustrade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.Goods;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.FavoriteService;
import com.campustrade.service.GoodsService;
import com.campustrade.vo.GoodsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品Controller
 */
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品发布、浏览、搜索等接口")
public class GoodsController {

    private final GoodsService goodsService;
    private final FavoriteService favoriteService;

    /**
     * 发布商品
     */
    @PostMapping("/publish")
    @Operation(summary = "发布商品")
    public Result<Void> publishGoods(@RequestBody Goods goods) {
        Long userId = SecurityUtil.getCurrentUserId();
        goods.setSellerId(userId);
        goodsService.publishGoods(goods);
        return Result.success("发布成功，等待审核", null);
    }

    /**
     * 编辑商品
     */
    @PutMapping("/update")
    @Operation(summary = "编辑商品")
    public Result<Void> updateGoods(@RequestBody Goods goods) {
        Long userId = SecurityUtil.getCurrentUserId();
        goods.setSellerId(userId);
        goodsService.updateGoods(goods);
        return Result.success("修改成功，需重新审核", null);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除商品")
    public Result<Void> deleteGoods(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        goodsService.deleteGoods(id, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 下架商品
     */
    @PutMapping("/offline/{id}")
    @Operation(summary = "下架商品")
    public Result<Void> offlineGoods(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        goodsService.offlineGoods(id, userId);
        return Result.success("已下架", null);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取商品详情")
    public Result<GoodsVO> getGoodsDetail(@PathVariable Long id) {
        // 增加浏览量
        goodsService.incrementViewCount(id);

        GoodsVO goodsVO = goodsService.getGoodsDetail(id);

        // 检查是否已收藏
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId != null) {
            goodsVO.setIsFavorited(favoriteService.isFavorited(userId, id));
        }
        goodsVO.setFavoriteCount(favoriteService.getFavoriteCount(id));

        return Result.success(goodsVO);
    }

    /**
     * 分页查询商品列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询商品列表")
    public Result<PageResult<GoodsVO>> getGoodsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {

        Page<Goods> pageParam = new Page<>(page, pageSize);
        Page<Goods> goodsPage = goodsService.lambdaQuery()
                .eq(categoryId != null, Goods::getCategoryId, categoryId)
                .eq(Goods::getStatus, StatusConstant.GOODS_ON_SALE)
                .and(keyword != null && !keyword.isEmpty(),
                        wrapper -> wrapper.like(Goods::getTitle, keyword)
                                .or().like(Goods::getDescription, keyword))
                .orderByDesc(Goods::getCreateTime)
                .page(pageParam);

        // 转换为VO
        List<GoodsVO> voList = goodsPage.getRecords().stream()
                .map(g -> goodsService.getGoodsDetail(g.getId()))
                .collect(Collectors.toList());

        PageResult<GoodsVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(goodsPage.getTotal());
        pageResult.setPage(goodsPage.getCurrent());
        pageResult.setPageSize(goodsPage.getSize());
        pageResult.setPages(goodsPage.getPages());

        return Result.success(pageResult);
    }

    /**
     * 获取我的商品列表
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的商品列表")
    public Result<List<Goods>> getMyGoods(@RequestParam(required = false) Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(goodsService.getSellerGoods(userId, status));
    }
}
