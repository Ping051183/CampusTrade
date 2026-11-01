package com.campustrade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 交易评价请求DTO
 */
@Data
@Schema(description = "交易评价请求")
public class ReviewDTO {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", required = true)
    private Long orderId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    @Schema(description = "评分 1-5", required = true)
    private Integer score;

    @Size(max = 500, message = "评价内容最多 500 字")
    @Schema(description = "评价内容")
    private String content;
}