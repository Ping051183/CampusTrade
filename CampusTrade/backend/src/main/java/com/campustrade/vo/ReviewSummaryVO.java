package com.campustrade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信用汇总VO
 */
@Data
@Schema(description = "用户信用汇总")
public class ReviewSummaryVO {

    @Schema(description = "平均分（无评价时为 0）")
    private Double avgScore;

    @Schema(description = "评价数量")
    private Long count;

    @Schema(description = "好评数量（4-5 星）")
    private Long goodCount;

    @Schema(description = "好评率（百分比，无评价时为 0）")
    private Double goodRate;
}