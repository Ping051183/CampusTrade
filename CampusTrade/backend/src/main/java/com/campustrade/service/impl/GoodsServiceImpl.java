package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.Category;
import com.campustrade.entity.Goods;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.CategoryMapper;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.GoodsService;
import com.campustrade.vo.GoodsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品Service实现类
 */
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    private final GoodsMapper goodsMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishGoods(Goods goods) {
        // 验证分类是否存在
        if (categoryMapper.selectById(goods.getCategoryId()) == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 设置初始状态为待审核
        goods.setStatus(StatusConstant.GOODS_PENDING);
        goods.setViewCount(0);
        goodsMapper.insert(goods);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoods(Goods goods) {
        Goods existing = goodsMapper.selectById(goods.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.GOODS_NOT_FOUND);
        }
        if (!existing.getSellerId().equals(goods.getSellerId())) {
            throw new BusinessException(ErrorCode.GOODS_NOT_YOURS);
        }

        // 更新后需要重新审核
        goods.setStatus(StatusConstant.GOODS_PENDING);
        goodsMapper.updateById(goods);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGoods(Long goodsId, Long userId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.GOODS_NOT_FOUND);
        }
        if (!goods.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GOODS_NOT_YOURS);
        }
        goodsMapper.deleteById(goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineGoods(Long goodsId, Long userId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.GOODS_NOT_FOUND);
        }
        if (!goods.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GOODS_NOT_YOURS);
        }
        goods.setStatus(StatusConstant.GOODS_OFFLINE);
        goodsMapper.updateById(goods);
    }

    @Override
    public GoodsVO getGoodsDetail(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.GOODS_NOT_FOUND);
        }

        GoodsVO goodsVO = new GoodsVO();
        BeanUtils.copyProperties(goods, goodsVO);

        // 查询分类信息
        Category category = categoryMapper.selectById(goods.getCategoryId());
        if (category != null) {
            goodsVO.setCategoryName(category.getCategoryName());
        }

        // 查询卖家信息
        User seller = userMapper.selectById(goods.getSellerId());
        if (seller != null) {
            goodsVO.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());
            goodsVO.setSellerAvatar(seller.getAvatar());
            goodsVO.setSellerSchool(seller.getSchool());
        }

        return goodsVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long goodsId) {
        goodsMapper.incrementViewCount(goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditGoods(Long goodsId, Integer status, String rejectReason) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.GOODS_NOT_FOUND);
        }

        goods.setStatus(status);
        goodsMapper.updateById(goods);
    }

    @Override
    public List<Goods> getSellerGoods(Long sellerId, Integer status) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getSellerId, sellerId);
        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }
        wrapper.orderByDesc(Goods::getCreateTime);
        return goodsMapper.selectList(wrapper);
    }
}
