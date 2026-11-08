package com.campustrade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度应在3-50之间")
    @Schema(description = "用户名", required = true)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度应在6-50之间")
    @Schema(description = "密码", required = true)
    private String password;

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Schema(description = "手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确，应为11位数字且以1开头")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    @Schema(description = "学号（12位：4位入学年份+6位专业班级+2位班内序号）", required = true)
    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^20\\d{10}$", message = "学号应为12位数字，如 202310610320")
    private String studentNo;

    @Schema(description = "学校（目前仅支持广州工商学院）", required = true)
    @NotBlank(message = "学校不能为空")
    private String school;
}
