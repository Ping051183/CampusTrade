package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.cache.CacheKeys;
import com.campustrade.cache.CacheService;
import com.campustrade.entity.Category;
import com.campustrade.mapper.CategoryMapper;
import com.campustrade.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 分类Service实现类
 * <p>
 * 分类属于极少变更的基础数据，使用缓存减少数据库查询
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    /** 分类列表缓存时长 */
    private static final Duration CATEGORY_TTL = Duration.ofMinutes(30);

    private final CacheService cacheService;

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> getAllCategories() {
        return (List<Category>) cacheService.getOrLoad(CacheKeys.CATEGORY_LIST, List.class, CATEGORY_TTL, () -> {
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(Category::getSort);
            return baseMapper.selectList(wrapper);
        });
    }
}