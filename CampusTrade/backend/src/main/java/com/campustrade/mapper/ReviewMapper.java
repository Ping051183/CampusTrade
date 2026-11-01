package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.entity.Review;
import com.campustrade.vo.ReviewSummaryVO;
import com.campustrade.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 交易评价Mapper接口
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 分页查询某个用户的被评价列表（带评价人、商品信息）
     */
    IPage<ReviewVO> selectByRateeId(Page<ReviewVO> page, @Param("rateeId") Long rateeId);

    /**
     * 统计某个用户的信用汇总（平均分、评价数、好评数）
     */
    ReviewSummaryVO selectSummary(@Param("rateeId") Long rateeId);
}