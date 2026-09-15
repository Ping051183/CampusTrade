package com.campustrade.service.impl;

import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.Category;
import com.campustrade.entity.Goods;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.CategoryMapper;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.vo.GoodsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoodsService 单元测试")
class GoodsServiceImplTest {

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private GoodsServiceImpl goodsService;

    @Captor
    private ArgumentCaptor<Goods> goodsCaptor;

    private User seller;
    private Category category;
    private Goods goods;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setUsername("seller");
        seller.setNickname("卖家");
        seller.setAvatar("/avatar.jpg");
        seller.setSchool("Test University");

        category = new Category();
        category.setId(1L);
        category.setCategoryName("数码产品");

        goods = new Goods();
        goods.setId(100L);
        goods.setTitle("Test Product");
        goods.setDescription("A test product");
        goods.setCategoryId(1L);
        goods.setPrice(new BigDecimal("99.99"));
        goods.setSellerId(1L);
        goods.setStatus(StatusConstant.GOODS_PENDING);
        goods.setViewCount(0);
    }

    @Nested
    @DisplayName("发布商品")
    class PublishGoodsTests {

        @Test
        @DisplayName("发布成功")
        void publishGoods_success() {
            Goods newGoods = new Goods();
            newGoods.setTitle("New Laptop");
            newGoods.setDescription("A brand new laptop");
            newGoods.setCategoryId(1L);
            newGoods.setPrice(new BigDecimal("4999.00"));
            newGoods.setSellerId(1L);

            when(categoryMapper.selectById(1L)).thenReturn(category);

            goodsService.publishGoods(newGoods);

            verify(goodsMapper).insert(goodsCaptor.capture());
            Goods saved = goodsCaptor.getValue();
            assertEquals("New Laptop", saved.getTitle());
            assertEquals(StatusConstant.GOODS_PENDING, saved.getStatus());
            assertEquals(0, saved.getViewCount());
        }

        @Test
        @DisplayName("分类不存在时抛出异常")
        void publishGoods_categoryNotFound() {
            Goods newGoods = new Goods();
            newGoods.setCategoryId(999L);
            newGoods.setSellerId(1L);

            when(categoryMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.publishGoods(newGoods));
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("编辑商品")
    class UpdateGoodsTests {

        @Test
        @DisplayName("编辑成功，状态重置为待审核")
        void updateGoods_success() {
            Goods existing = new Goods();
            existing.setId(100L);
            existing.setSellerId(1L);
            existing.setStatus(StatusConstant.GOODS_ON_SALE);

            Goods updated = new Goods();
            updated.setId(100L);
            updated.setTitle("Updated Title");
            updated.setDescription("Updated description");
            updated.setSellerId(1L);

            when(goodsMapper.selectById(100L)).thenReturn(existing);

            goodsService.updateGoods(updated);

            verify(goodsMapper).updateById(goodsCaptor.capture());
            Goods saved = goodsCaptor.getValue();
            assertEquals("Updated Title", saved.getTitle());
            assertEquals(StatusConstant.GOODS_PENDING, saved.getStatus());
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void updateGoods_notFound() {
            Goods updated = new Goods();
            updated.setId(999L);

            when(goodsMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.updateGoods(updated));
            assertEquals(ErrorCode.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非卖家编辑时抛出异常")
        void updateGoods_notOwner() {
            Goods existing = new Goods();
            existing.setId(100L);
            existing.setSellerId(1L);

            Goods updated = new Goods();
            updated.setId(100L);
            updated.setSellerId(2L);

            when(goodsMapper.selectById(100L)).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.updateGoods(updated));
            assertEquals(ErrorCode.GOODS_NOT_YOURS.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("删除商品")
    class DeleteGoodsTests {

        @Test
        @DisplayName("卖家删除自己的商品")
        void deleteGoods_own() {
            when(goodsMapper.selectById(100L)).thenReturn(goods);

            goodsService.deleteGoods(100L, 1L);

            verify(goodsMapper).deleteById(100L);
        }

        @Test
        @DisplayName("非卖家删除时抛出异常")
        void deleteGoods_notOwner() {
            when(goodsMapper.selectById(100L)).thenReturn(goods);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.deleteGoods(100L, 2L));
            assertEquals(ErrorCode.GOODS_NOT_YOURS.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("下架商品")
    class OfflineGoodsTests {

        @Test
        @DisplayName("卖家下架自己的商品")
        void offlineGoods_own() {
            when(goodsMapper.selectById(100L)).thenReturn(goods);

            goodsService.offlineGoods(100L, 1L);

            verify(goodsMapper).updateById(goodsCaptor.capture());
            assertEquals(StatusConstant.GOODS_OFFLINE, goodsCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("非卖家下架时抛出异常")
        void offlineGoods_notOwner() {
            when(goodsMapper.selectById(100L)).thenReturn(goods);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.offlineGoods(100L, 2L));
            assertEquals(ErrorCode.GOODS_NOT_YOURS.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("商品详情")
    class GetGoodsDetailTests {

        @Test
        @DisplayName("返回完整商品详情")
        void getGoodsDetail_success() {
            goods.setImages("/img1.jpg,/img2.jpg");
            goods.setStatus(StatusConstant.GOODS_ON_SALE);

            when(goodsMapper.selectById(100L)).thenReturn(goods);
            when(categoryMapper.selectById(1L)).thenReturn(category);
            when(userMapper.selectById(1L)).thenReturn(seller);

            GoodsVO vo = goodsService.getGoodsDetail(100L);

            assertNotNull(vo);
            assertEquals("Test Product", vo.getTitle());
            assertEquals("数码产品", vo.getCategoryName());
            assertEquals("卖家", vo.getSellerName());
            assertEquals("Test University", vo.getSellerSchool());
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void getGoodsDetail_notFound() {
            when(goodsMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.getGoodsDetail(999L));
            assertEquals(ErrorCode.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("增加浏览量")
    void incrementViewCount() {
        goodsService.incrementViewCount(100L);

        verify(goodsMapper).incrementViewCount(100L);
    }

    @Nested
    @DisplayName("商品审核")
    class AuditGoodsTests {

        @Test
        @DisplayName("审核通过")
        void auditGoods_approve() {
            when(goodsMapper.selectById(100L)).thenReturn(goods);

            goodsService.auditGoods(100L, StatusConstant.GOODS_ON_SALE, null);

            verify(goodsMapper).updateById(goodsCaptor.capture());
            assertEquals(StatusConstant.GOODS_ON_SALE, goodsCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("审核驳回")
        void auditGoods_reject() {
            when(goodsMapper.selectById(100L)).thenReturn(goods);

            goodsService.auditGoods(100L, StatusConstant.GOODS_REJECTED, "图片不清晰");

            verify(goodsMapper).updateById(goodsCaptor.capture());
            assertEquals(StatusConstant.GOODS_REJECTED, goodsCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("商品不存在时抛出异常")
        void auditGoods_notFound() {
            when(goodsMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> goodsService.auditGoods(999L, StatusConstant.GOODS_ON_SALE, null));
            assertEquals(ErrorCode.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("卖家商品列表")
    class GetSellerGoodsTests {

        @Test
        @DisplayName("查询所有商品")
        void getSellerGoods_all() {
            List<Goods> mockList = List.of(goods);
            when(goodsMapper.selectList(any())).thenReturn(mockList);

            List<Goods> result = goodsService.getSellerGoods(1L, null);

            assertEquals(1, result.size());
            assertEquals("Test Product", result.get(0).getTitle());
        }

        @Test
        @DisplayName("按状态筛选")
        void getSellerGoods_byStatus() {
            List<Goods> mockList = List.of(goods);
            when(goodsMapper.selectList(any())).thenReturn(mockList);

            List<Goods> result = goodsService.getSellerGoods(1L, StatusConstant.GOODS_PENDING);

            assertEquals(1, result.size());
        }
    }
}
