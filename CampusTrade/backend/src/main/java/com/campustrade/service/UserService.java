package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.dto.UpdatePasswordDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.entity.User;
import com.campustrade.vo.LoginVO;
import com.campustrade.vo.UserVO;

/**
 * 用户Service接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 获取当前登录用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 修改个人信息
     */
    void updateUserInfo(Long userId, UserUpdateDTO updateDTO);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, UpdatePasswordDTO updatePasswordDTO);

    /**
     * 上传头像
     */
    String uploadAvatar(Long userId, String avatarUrl);
}
