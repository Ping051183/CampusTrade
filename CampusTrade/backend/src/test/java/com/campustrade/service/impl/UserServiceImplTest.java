package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.constant.RoleConstant;
import com.campustrade.constant.StatusConstant;
import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.dto.UpdatePasswordDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.JwtTokenProvider;
import com.campustrade.vo.LoginVO;
import com.campustrade.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtTokenProvider);
    }

    @Nested
    @DisplayName("注册")
    class RegisterTests {

        @Test
        @DisplayName("注册成功")
        void register_success() {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("password123");
            dto.setNickname("New User");
            dto.setPhone("13800138000");
            dto.setSchool("Test University");

            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            userService.register(dto);

            verify(userMapper).insert(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertEquals("newuser", saved.getUsername());
            assertTrue(passwordEncoder.matches("password123", saved.getPassword()));
            assertEquals(RoleConstant.USER, saved.getRole());
            assertEquals(StatusConstant.USER_ENABLED, saved.getStatus());
        }

        @Test
        @DisplayName("用户名已存在时抛出异常")
        void register_usernameExists() {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("existing");
            dto.setPassword("password123");

            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(dto));
            assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("登录")
    class LoginTests {

        @Test
        @DisplayName("登录成功返回 LoginVO")
        void login_success() {
            String rawPassword = "password123";
            String encodedPassword = passwordEncoder.encode(rawPassword);

            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword(encodedPassword);
            user.setNickname("Test User");
            user.setRole(RoleConstant.USER);
            user.setStatus(StatusConstant.USER_ENABLED);

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword(rawPassword);

            when(userMapper.findByUsername("testuser")).thenReturn(user);
            when(jwtTokenProvider.generateToken(1L, RoleConstant.USER)).thenReturn("mock-jwt-token");

            LoginVO loginVO = userService.login(dto);

            assertNotNull(loginVO);
            assertEquals("mock-jwt-token", loginVO.getToken());
            assertEquals(1L, loginVO.getUserId());
            assertEquals("testuser", loginVO.getUsername());
            assertEquals("Test User", loginVO.getNickname());
            assertEquals(RoleConstant.USER, loginVO.getRole());
        }

        @Test
        @DisplayName("用户名不存在时抛出异常")
        void login_userNotFound() {
            LoginDTO dto = new LoginDTO();
            dto.setUsername("nonexistent");
            dto.setPassword("password123");

            when(userMapper.findByUsername("nonexistent")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(dto));
            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("用户被禁用时抛出异常")
        void login_userDisabled() {
            User user = new User();
            user.setId(1L);
            user.setUsername("disabled");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setStatus(StatusConstant.USER_DISABLED);

            LoginDTO dto = new LoginDTO();
            dto.setUsername("disabled");
            dto.setPassword("password123");

            when(userMapper.findByUsername("disabled")).thenReturn(user);

            BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(dto));
            assertEquals(ErrorCode.USER_DISABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("密码错误时抛出异常")
        void login_wrongPassword() {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword(passwordEncoder.encode("correctPassword"));
            user.setStatus(StatusConstant.USER_ENABLED);

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("wrongPassword");

            when(userMapper.findByUsername("testuser")).thenReturn(user);

            BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(dto));
            assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("获取用户信息")
    class GetCurrentUserTests {

        @Test
        @DisplayName("用户存在时返回 UserVO")
        void getCurrentUser_success() {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setNickname("Test User");
            user.setAvatar("/avatar.jpg");
            user.setSchool("Test University");

            when(userMapper.selectById(1L)).thenReturn(user);

            UserVO userVO = userService.getCurrentUser(1L);

            assertNotNull(userVO);
            assertEquals(1L, userVO.getId());
            assertEquals("testuser", userVO.getUsername());
            assertEquals("Test User", userVO.getNickname());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void getCurrentUser_notFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> userService.getCurrentUser(999L));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("修改个人信息")
    class UpdateUserInfoTests {

        @Test
        @DisplayName("更新成功")
        void updateUserInfo_success() {
            User existing = new User();
            existing.setId(1L);
            existing.setUsername("testuser");

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("New Nickname");
            dto.setPhone("13900139000");
            dto.setSchool("New School");

            when(userMapper.selectById(1L)).thenReturn(existing);

            userService.updateUserInfo(1L, dto);

            verify(userMapper).updateById(userCaptor.capture());
            User updated = userCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals("New Nickname", updated.getNickname());
            assertEquals("13900139000", updated.getPhone());
            assertEquals("New School", updated.getSchool());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void updateUserInfo_notFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateUserInfo(999L, new UserUpdateDTO()));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("修改密码")
    class UpdatePasswordTests {

        @Test
        @DisplayName("密码修改成功")
        void updatePassword_success() {
            String oldPassword = "oldPass123";
            String newPassword = "newPass456";
            String encodedOld = passwordEncoder.encode(oldPassword);

            User user = new User();
            user.setId(1L);
            user.setPassword(encodedOld);

            UpdatePasswordDTO dto = new UpdatePasswordDTO();
            dto.setOldPassword(oldPassword);
            dto.setNewPassword(newPassword);

            when(userMapper.selectById(1L)).thenReturn(user);

            userService.updatePassword(1L, dto);

            verify(userMapper).updateById(userCaptor.capture());
            User updated = userCaptor.getValue();
            assertTrue(passwordEncoder.matches(newPassword, updated.getPassword()));
        }

        @Test
        @DisplayName("原密码错误时抛出异常")
        void updatePassword_wrongOldPassword() {
            String encodedPassword = passwordEncoder.encode("correctOldPass");

            User user = new User();
            user.setId(1L);
            user.setPassword(encodedPassword);

            UpdatePasswordDTO dto = new UpdatePasswordDTO();
            dto.setOldPassword("wrongOldPass");
            dto.setNewPassword("newPass456");

            when(userMapper.selectById(1L)).thenReturn(user);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(1L, dto));
            assertEquals(ErrorCode.OLD_PASSWORD_ERROR.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("上传头像")
    class UploadAvatarTests {

        @Test
        @DisplayName("头像更新成功")
        void uploadAvatar_success() {
            User user = new User();
            user.setId(1L);

            when(userMapper.selectById(1L)).thenReturn(user);

            String avatarUrl = "/uploads/avatar.jpg";
            String result = userService.uploadAvatar(1L, avatarUrl);

            assertEquals(avatarUrl, result);
            verify(userMapper).updateById(userCaptor.capture());
            assertEquals(avatarUrl, userCaptor.getValue().getAvatar());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void uploadAvatar_notFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.uploadAvatar(999L, "/avatar.jpg"));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
