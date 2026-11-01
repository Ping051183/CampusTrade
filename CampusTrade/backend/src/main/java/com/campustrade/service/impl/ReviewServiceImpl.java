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
import com.campustrade.service.ReviewService;
import com.campustrade.vo.ReviewSummaryVO;
import com.campustrade.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 交易评价Service实现类
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReview(Long orderId, Long raterId, Integer score, String content) {
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评分必须在 1-5 星之间");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        boolean isBuyer = order.getBuyerId().equals(raterId);
        boolean isSeller = order.getSellerId().equals(raterId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 只有交易完成（已收货/已完成）的订单才能评价
        if (!StatusConstant.ORDER_RECEIVED.equals(order.getStatus())
                && !StatusConstant.ORDER_COMPLETED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "订单完成后才能评价");
        }

        // CAS 标记已评价，保证同一方对同一订单只能评价一次（并发下也只有一次成功）
        int marked = isBuyer ? orderMapper.markBuyerRated(orderId) : orderMapper.markSellerRated(orderId);
        if (marked == 0) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_RATED);
        }

        Long rateeId = isBuyer ? order.getSellerId() : order.getBuyerId();

        Review review = new Review();
        review.setOrderId(orderId);
        review.setGoodsId(order.getGoodsId());
        review.setRaterId(raterId);
        review.setRateeId(rateeId);
        review.setScore(score);
        review.setContent(content);
        reviewMapper.insert(review);

        notificationService.send(rateeId, "REVIEW_RECEIVED", "收到一条新评价",
                "评分 " + score + " 星" + (content == null || content.isBlank() ? "" : "：" + content),
                orderId);
    }

    @Override
    public PageResult<ReviewVO> pageByRateeId(Long rateeId, int page, int pageSize) {
        return PageResult.from(reviewMapper.selectByRateeId(new Page<>(page, pageSize), rateeId));
    }

    @Override
    public ReviewSummaryVO summary(Long rateeId) {
        ReviewSummaryVO summary = reviewMapper.selectSummary(rateeId);
        if (summary == null) {
            summary = new ReviewSummaryVO();
            summary.setAvgScore(0.0);
            summary.setCount(0L);
            summary.setGoodCount(0L);
        }
        long count = summary.getCount() == null ? 0 : summary.getCount();
        long good = summary.getGoodCount() == null ? 0 : summary.getGoodCount();
        summary.setGoodRate(count == 0 ? 0.0 : Math.round(good * 1000.0 / count) / 10.0);
        if (summary.getAvgScore() == null) {
            summary.setAvgScore(0.0);
        }
        return summary;
    }
}