package com.campustrade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息VO（含分类和卖家信息）
 */
@Data
@Schema(description = "商品信息（含分类和卖家信息）")
public class GoodsVO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "卖家名称")
    private String sellerName;

    @Schema(description = "卖家头像")
    private String sellerAvatar;

    @Schema(description = "卖家学校")
    private String sellerSchool;

    @Schema(description = "图片URL(多个用逗号分隔)")
    private String images;

    @Schema(description = "状态: 0-待审核 1-在售 2-已下架 3-已售出 4-审核驳回")
    private Integer status;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "是否已收藏")
    private Boolean isFavorited;

    @Schema(description = "收藏数")
    private Long favoriteCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
