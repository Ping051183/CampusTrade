package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Category;

import java.util.List;

/**
 * 分类Service接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类（按排序号排列）
     */
    List<Category> getAllCategories();
}
