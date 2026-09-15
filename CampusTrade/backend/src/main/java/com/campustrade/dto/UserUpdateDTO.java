package com.campustrade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信息更新DTO
 */
@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateDTO {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "学校")
    private String school;
}
