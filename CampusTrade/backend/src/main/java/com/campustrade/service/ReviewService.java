package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.vo.ReviewVO;
import com.campustrade.vo.ReviewSummaryVO;

/**
 * 交易评价Service
 */
public interface ReviewService {

    /**
     * 对已完成（已收货）的订单发表评价。
     * 买家评价卖家、卖家评价买家均可，每方对同一订单只能评价一次。
     */
    void createReview(Long orderId, Long raterId, Integer score, String content);

    /**
     * 分页查询某个用户收到的评价
     */
    PageResult<ReviewVO> pageByRateeId(Long rateeId, int page, int pageSize);

    /**
     * 查询某个用户的信用汇总
     */
    ReviewSummaryVO summary(Long rateeId);
}