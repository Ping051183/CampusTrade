package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.entity.Favorite;
import com.campustrade.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 收藏Mapper接口
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 分页查询用户的收藏商品列表
     */
    IPage<Goods> selectFavoriteGoods(Page<Goods> page, @Param("userId") Long userId);

    /**
     * 统计收藏数
     */
    Long countByGoodsId(@Param("goodsId") Long goodsId);
}
