package com.campustrade.controller;

import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.exception.GlobalExceptionHandler;
import com.campustrade.service.UserService;
import com.campustrade.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 接口测试（MockMvc）：验证注册/登录的入参校验与统一返回结构。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 接口测试")
class AuthControllerTest {

    private static final String VALID_REGISTER = """
            {"username":"zhangsan","password":"123456","studentNo":"202310610320","school":"广州工商学院"}
            """;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("注册")
    class RegisterTests {

        @Test
        @DisplayName("合法入参注册成功，DTO 原样透传")
        void register_success() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("注册成功"));

            ArgumentCaptor<RegisterDTO> captor = ArgumentCaptor.forClass(RegisterDTO.class);
            verify(userService).register(captor.capture());
            assertEquals("202310610320", captor.getValue().getStudentNo());
            assertEquals("广州工商学院", captor.getValue().getSchool());
        }

        @Test
        @DisplayName("学号不足 12 位被 @Pattern 拦截，返回 400")
        void register_invalidStudentNo() throws Exception {
            String body = """
                    {"username":"zhangsan","password":"123456","studentNo":"20231061032","school":"广州工商学院"}
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("学号应为12位数字，如 202310610320"));

            verify(userService, never()).register(any());
        }

        @Test
        @DisplayName("学校不是广州工商学院时返回业务码 1008")
        void register_schoolNotSupported() throws Exception {
            doThrow(new BusinessException(ErrorCode.SCHOOL_NOT_SUPPORTED, "目前仅支持广州工商学院学生注册"))
                    .when(userService).register(any());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SCHOOL_NOT_SUPPORTED.getCode()));
        }
    }

    @Nested
    @DisplayName("登录")
    class LoginTests {

        @Test
        @DisplayName("登录成功返回 token 与用户信息")
        void login_success() throws Exception {
            LoginVO loginVO = new LoginVO();
            loginVO.setToken("jwt-token-value");
            loginVO.setUserId(2L);
            loginVO.setUsername("lwx");
            when(userService.login(any())).thenReturn(loginVO);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"lwx\",\"password\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").value("jwt-token-value"))
                    .andExpect(jsonPath("$.data.userId").value(2));
        }

        @Test
        @DisplayName("密码为空时被 @NotBlank 拦截，返回 400")
        void login_passwordBlank() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"lwx\",\"password\":\"\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));

            verify(userService, never()).login(any(LoginDTO.class));
        }
    }
}