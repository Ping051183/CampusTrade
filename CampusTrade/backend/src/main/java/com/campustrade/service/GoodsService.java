package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Goods;
import com.campustrade.vo.GoodsVO;

import java.util.List;

/**
 * 商品Service接口
 */
public interface GoodsService extends IService<Goods> {

    /**
     * 发布商品
     */
    void publishGoods(Goods goods);

    /**
     * 编辑商品
     */
    void updateGoods(Goods goods);

    /**
     * 删除商品（逻辑删除/下架）
     */
    void deleteGoods(Long goodsId, Long userId);

    /**
     * 下架商品
     */
    void offlineGoods(Long goodsId, Long userId);

    /**
     * 获取商品详情（含卖家信息和分类信息）
     */
    GoodsVO getGoodsDetail(Long goodsId);

    /**
     * 增加浏览量
     */
    void incrementViewCount(Long goodsId);

    /**
     * 审核商品
     */
    void auditGoods(Long goodsId, Integer status, String rejectReason);

    /**
     * 查询卖家的商品列表
     */
    List<Goods> getSellerGoods(Long sellerId, Integer status);
}
