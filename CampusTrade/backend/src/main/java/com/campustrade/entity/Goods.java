package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Data
@TableName("goods")
public class Goods {

    /** 商品ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 分类ID */
    private Long categoryId;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 卖家ID */
    private Long sellerId;

    /** 图片URL(多个用逗号分隔) */
    private String images;

    /** 状态: 0-待审核 1-在售 2-已下架 3-已售出 4-审核驳回 */
    private Integer status;

    /** 浏览次数 */
    private Integer viewCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
