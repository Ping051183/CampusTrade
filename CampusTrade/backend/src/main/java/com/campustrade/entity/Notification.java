package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知实体类
 */
@Data
@TableName("notification")
public class Notification {

    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人ID */
    private Long userId;

    /** 通知类型: ORDER_CREATED/ORDER_PAID/ORDER_SHIPPED/REFUND_APPLY... */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 关联ID(订单/商品等) */
    private Long relatedId;

    /** 是否已读: 0-否 1-是 */
    private Integer isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}