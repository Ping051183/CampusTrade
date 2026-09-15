package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Favorite;
import com.campustrade.entity.Goods;

import java.util.List;

/**
 * 收藏Service接口
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 添加收藏
     */
    void addFavorite(Long userId, Long goodsId);

    /**
     * 取消收藏
     */
    void cancelFavorite(Long userId, Long goodsId);

    /**
     * 检查是否已收藏
     */
    boolean isFavorited(Long userId, Long goodsId);

    /**
     * 获取用户收藏的商品列表
     */
    List<Goods> getUserFavorites(Long userId);

    /**
     * 获取商品收藏数
     */
    Long getFavoriteCount(Long goodsId);
}
