package com.campustrade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易评价VO（带评价人、商品信息）
 */
@Data
@Schema(description = "交易评价")
public class ReviewVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品标题")
    private String goodsTitle;

    @Schema(description = "商品图片")
    private String goodsImages;

    @Schema(description = "评价人ID")
    private Long raterId;

    @Schema(description = "评价人昵称")
    private String raterName;

    @Schema(description = "评价人头像")
    private String raterAvatar;

    @Schema(description = "被评价人ID")
    private Long rateeId;

    @Schema(description = "评分 1-5")
    private Integer score;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "评价时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}