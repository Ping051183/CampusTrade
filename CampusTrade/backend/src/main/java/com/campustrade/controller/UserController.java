package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.dto.UpdatePasswordDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.UserService;
import com.campustrade.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户信息Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息管理接口")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getCurrentUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(userService.getCurrentUser(userId));
    }

    /**
     * 修改个人信息
     */
    @PutMapping("/update")
    @Operation(summary = "修改个人信息")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserUpdateDTO updateDTO) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.updateUserInfo(userId, updateDTO);
        return Result.success("修改成功", null);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.updatePassword(userId, updatePasswordDTO);
        return Result.success("密码修改成功", null);
    }

    /**
     * 更新头像
     */
    @PutMapping("/avatar")
    @Operation(summary = "更新头像")
    public Result<String> updateAvatar(@RequestBody Map<String, String> body) {
        String avatarUrl = body.get("avatarUrl");
        Long userId = SecurityUtil.getCurrentUserId();
        String url = userService.uploadAvatar(userId, avatarUrl);
        return Result.success("头像更新成功", url);
    }
}
