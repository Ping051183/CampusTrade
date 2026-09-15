package com.campustrade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会话VO（会话列表使用）
 */
@Data
@Schema(description = "会话信息")
public class ConversationVO {

    @Schema(description = "对方用户ID")
    private Long otherUserId;

    @Schema(description = "对方昵称")
    private String otherUserName;

    @Schema(description = "对方头像")
    private String otherUserAvatar;

    @Schema(description = "关联商品ID")
    private Long goodsId;

    @Schema(description = "商品标题")
    private String goodsTitle;

    @Schema(description = "商品图片")
    private String goodsImage;

    @Schema(description = "商品价格")
    private BigDecimal goodsPrice;

    @Schema(description = "最后一条消息内容")
    private String lastMessage;

    @Schema(description = "最后一条消息的图片URL")
    private String lastMessageImageUrl;

    @Schema(description = "最后消息时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;

    @Schema(description = "未读消息数")
    private Integer unreadCount;
}
