package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.entity.Favorite;
import com.campustrade.entity.Goods;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.FavoriteMapper;
import com.campustrade.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收藏Service实现类
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final FavoriteMapper favoriteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long goodsId) {
        // 检查是否已收藏
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getGoodsId, goodsId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.FAVORITE_EXISTS);
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setGoodsId(goodsId);
        favoriteMapper.insert(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelFavorite(Long userId, Long goodsId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getGoodsId, goodsId);
        Favorite favorite = favoriteMapper.selectOne(wrapper);
        if (favorite == null) {
            throw new BusinessException(ErrorCode.FAVORITE_NOT_FOUND);
        }
        favoriteMapper.deleteById(favorite.getId());
    }

    @Override
    public boolean isFavorited(Long userId, Long goodsId) {
        if (userId == null) return false;
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getGoodsId, goodsId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Goods> getUserFavorites(Long userId) {
        return favoriteMapper.selectFavoriteGoods(new Page<>(1, 100), userId).getRecords();
    }

    @Override
    public Long getFavoriteCount(Long goodsId) {
        return favoriteMapper.countByGoodsId(goodsId);
    }
}
