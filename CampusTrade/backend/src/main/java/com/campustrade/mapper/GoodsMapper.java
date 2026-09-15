package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品Mapper接口
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 分页查询商品列表（关联分类和卖家信息）
     */
    IPage<Goods> selectGoodsWithDetails(Page<Goods> page, @Param("categoryId") Long categoryId,
                                        @Param("status") Integer status, @Param("keyword") String keyword);

    /**
     * 根据卖家ID查询商品列表
     */
    IPage<Goods> selectBySellerId(Page<Goods> page, @Param("sellerId") Long sellerId);

    /**
     * 统计在线商品总数
     */
    Long countOnSale();

    /**
     * 增加浏览次数
     */
    int incrementViewCount(@Param("id") Long id);

    /**
     * 统计待审核商品数量
     */
    Long countPending();
}
