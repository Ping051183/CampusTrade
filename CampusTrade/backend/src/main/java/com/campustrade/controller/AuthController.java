package com.campustrade.controller;

import com.campustrade.annotation.RateLimit;
import com.campustrade.common.Result;
import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.service.UserService;
import com.campustrade.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证Controller
 * 提供登录、注册等认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录注册接口")
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @RateLimit(limit = 5, windowSeconds = 60, key = "register")
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    /**
     * 用户登录
     */
    @RateLimit(limit = 10, windowSeconds = 60, key = "login")
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }
}
