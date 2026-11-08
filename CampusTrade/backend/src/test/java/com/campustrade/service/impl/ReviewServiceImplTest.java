package com.campustrade.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.PageResult;
import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.Order;
import com.campustrade.entity.Review;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.mapper.ReviewMapper;
import com.campustrade.service.NotificationService;
import com.campustrade.vo.ReviewSummaryVO;
import com.campustrade.vo.ReviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReviewServiceImpl 单元测试：评价权限、订单状态、重复评价、信用汇总计算。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl 单元测试")
class ReviewServiceImplTest {

    private static final Long ORDER_ID = 8L;
    private static final Long BUYER_ID = 2L;
    private static final Long SELLER_ID = 3L;
    private static final Long GOODS_ID = 5L;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private NotificationService notificationService;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(reviewMapper, orderMapper, notificationService);
    }

    private Order receivedOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setGoodsId(GOODS_ID);
        order.setStatus(StatusConstant.ORDER_RECEIVED);
        return order;
    }

    @Nested
    @DisplayName("提交评价校验")
    class ValidationTests {

        @Test
        @DisplayName("评分为空或越界时返回 400，不查询订单")
        void create_scoreInvalid() {
            BusinessException nullScore = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(ORDER_ID, BUYER_ID, null, "x"));
            assertEquals(ErrorCode.BAD_REQUEST.getCode(), nullScore.getCode());

            BusinessException zero = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(ORDER_ID, BUYER_ID, 0, "x"));
            assertEquals(ErrorCode.BAD_REQUEST.getCode(), zero.getCode());

            BusinessException six = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(ORDER_ID, BUYER_ID, 6, "x"));
            assertEquals(ErrorCode.BAD_REQUEST.getCode(), six.getCode());

            verify(orderMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("订单不存在时返回 5001")
        void create_orderNotFound() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(ORDER_ID, BUYER_ID, 5, "很好"));
            assertEquals(ErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非买卖双方不可评价，返回 403")
        void create_notParticipant() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(receivedOrder());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(ORDER_ID, 99L, 5, "很好"));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("订单未完成（待付款/已付款/已发货）时不可评价")
        void create_orderNotFinished() {
            Order order = receivedOrder();
            order.setStatus(StatusConstant.ORDER_PAID);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(ORDER_ID, BUYER_ID, 5, "很好"));
            assertEquals(ErrorCode.ORDER_STATUS_ERROR.getCode(), ex.getCode());
            verify(orderMapper, never()).markBuyerRated(anyLong());
        }

        @Test
        @DisplayName("已评价过的订单再次评价返回 5009")
        void create_alreadyRated() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(receivedOrder());
            when(orderMapper.markBuyerRated(ORDER_ID)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(ORDER_ID, BUYER_ID, 5, "很好"));
            assertEquals(ErrorCode.ORDER_ALREADY_RATED.getCode(), ex.getCode());
            verify(reviewMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("提交评价成功")
    class CreateSuccessTests {

        @Test
        @DisplayName("买家评价卖家：写入评价并通知卖家")
        void create_byBuyer() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(receivedOrder());
            when(orderMapper.markBuyerRated(ORDER_ID)).thenReturn(1);

            reviewService.createReview(ORDER_ID, BUYER_ID, 5, "成色很好");

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewMapper).insert(captor.capture());
            Review saved = captor.getValue();
            assertEquals(ORDER_ID, saved.getOrderId());
            assertEquals(GOODS_ID, saved.getGoodsId());
            assertEquals(BUYER_ID, saved.getRaterId());
            assertEquals(SELLER_ID, saved.getRateeId());
            assertEquals(5, saved.getScore().intValue());

            verify(notificationService).send(eq(SELLER_ID), eq("REVIEW_RECEIVED"),
                    eq("收到一条新评价"), eq("评分 5 星：成色很好"), eq(ORDER_ID));
        }

        @Test
        @DisplayName("卖家评价买家：CAS 标记 seller_rated 并通知买家")
        void create_bySeller() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(receivedOrder());
            when(orderMapper.markSellerRated(ORDER_ID)).thenReturn(1);

            reviewService.createReview(ORDER_ID, SELLER_ID, 4, "买家爽快");

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewMapper).insert(captor.capture());
            assertEquals(SELLER_ID, captor.getValue().getRaterId());
            assertEquals(BUYER_ID, captor.getValue().getRateeId());

            verify(orderMapper).markSellerRated(ORDER_ID);
            verify(orderMapper, never()).markBuyerRated(anyLong());
            verify(notificationService).send(eq(BUYER_ID), eq("REVIEW_RECEIVED"),
                    anyString(), anyString(), eq(ORDER_ID));
        }

        @Test
        @DisplayName("内容为空时通知正文只包含星级")
        void create_blankContent() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(receivedOrder());
            when(orderMapper.markBuyerRated(ORDER_ID)).thenReturn(1);

            reviewService.createReview(ORDER_ID, BUYER_ID, 3, "  ");

            verify(notificationService).send(eq(SELLER_ID), eq("REVIEW_RECEIVED"),
                    eq("收到一条新评价"), eq("评分 3 星"), eq(ORDER_ID));
        }
    }

    @Nested
    @DisplayName("查询与信用汇总")
    class QueryTests {

        @Test
        @DisplayName("按被评价人分页查询")
        void pageByRateeId() {
            Page<ReviewVO> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(2);
            when(reviewMapper.selectByRateeId(any(Page.class), eq(SELLER_ID))).thenReturn(page);

            PageResult<ReviewVO> result = reviewService.pageByRateeId(SELLER_ID, 1, 10);

            assertEquals(2, result.getTotal());
            verify(reviewMapper).selectByRateeId(any(Page.class), eq(SELLER_ID));
        }

        @Test
        @DisplayName("无评价时汇总返回 0 分 0 条 0%")
        void summary_empty() {
            when(reviewMapper.selectSummary(SELLER_ID)).thenReturn(null);

            ReviewSummaryVO summary = reviewService.summary(SELLER_ID);

            assertEquals(0.0, summary.getAvgScore());
            assertEquals(0L, summary.getCount());
            assertEquals(0.0, summary.getGoodRate());
        }

        @Test
        @DisplayName("好评率按 4-5 星计算并保留一位小数")
        void summary_computesGoodRate() {
            ReviewSummaryVO raw = new ReviewSummaryVO();
            raw.setAvgScore(4.0);
            raw.setCount(3L);
            raw.setGoodCount(2L);
            when(reviewMapper.selectSummary(SELLER_ID)).thenReturn(raw);

            ReviewSummaryVO summary = reviewService.summary(SELLER_ID);

            assertEquals(66.7, summary.getGoodRate());
            assertEquals(4.0, summary.getAvgScore());
        }

        @Test
        @DisplayName("count 为 0 时好评率为 0，避免除零")
        void summary_zeroCount() {
            ReviewSummaryVO raw = new ReviewSummaryVO();
            raw.setAvgScore(5.0);
            raw.setCount(0L);
            raw.setGoodCount(0L);
            when(reviewMapper.selectSummary(SELLER_ID)).thenReturn(raw);

            assertEquals(0.0, reviewService.summary(SELLER_ID).getGoodRate());
        }
    }
}