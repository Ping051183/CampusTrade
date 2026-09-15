package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体类
 */
@Data
@TableName("comment")
public class Comment {

    /** 评论ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long goodsId;

    /** 用户ID */
    private Long userId;

    /** 评论内容 */
    private String content;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
