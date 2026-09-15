package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报实体类
 */
@Data
@TableName("report")
public class Report {

    /** 举报ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long goodsId;

    /** 举报人ID */
    private Long reporterId;

    /** 举报原因 */
    private String reason;

    /** 处理状态: 0-待处理 1-已处理 2-驳回 */
    private Integer status;

    /** 处理结果 */
    private String handleResult;

    /** 处理人ID */
    private Long handlerId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
