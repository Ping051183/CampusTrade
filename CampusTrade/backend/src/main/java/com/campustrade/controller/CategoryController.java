package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.Category;
import com.campustrade.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类Controller
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "商品分类接口")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有分类
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有分类")
    public Result<List<Category>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }
}
