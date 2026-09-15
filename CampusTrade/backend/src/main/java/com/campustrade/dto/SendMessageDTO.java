package com.campustrade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息DTO
 */
@Data
@Schema(description = "发送消息请求")
public class SendMessageDTO {

    @NotNull(message = "接收者ID不能为空")
    @Schema(description = "接收者ID")
    private Long receiverId;

    @Schema(description = "关联商品ID")
    private Long goodsId;

    @Schema(description = "消息内容（文本消息时必填）")
    private String content;

    @Schema(description = "图片URL（图片消息时必填）")
    private String imageUrl;
}
