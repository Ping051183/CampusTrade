package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.dto.ReviewDTO;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.exception.GlobalExceptionHandler;
import com.campustrade.service.ReviewService;
import com.campustrade.vo.ReviewSummaryVO;
import com.campustrade.vo.ReviewVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReviewController 接口测试（MockMvc）：验证评价提交、评分校验与公开信用查询。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController 接口测试")
class ReviewControllerTest {

    private static final Long USER_ID = 2L;
    private static final Long ORDER_ID = 8L;

    @Mock
    private ReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("提交评价")
    class CreateReviewTests {

        @Test
        @DisplayName("合法评价：当前用户 ID、订单、评分、内容透传给 Service")
        void create_success() throws Exception {
            mockMvc.perform(post("/api/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":8,\"score\":5,\"content\":\"成色很好\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("评价成功"));

            ArgumentCaptor<Long> orderCaptor = ArgumentCaptor.forClass(Long.class);
            verify(reviewService).createReview(orderCaptor.capture(), eq(USER_ID), eq(5), eq("成色很好"));
            assertEquals(ORDER_ID, orderCaptor.getValue());
        }

        @Test
        @DisplayName("评分 6 超出 1-5 范围被 @Max 拦截，返回 400")
        void create_scoreOutOfRange() throws Exception {
            mockMvc.perform(post("/api/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":8,\"score\":6,\"content\":\"x\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));

            verify(reviewService, never()).createReview(anyLong(), anyLong(), anyInt(), any());
        }

        @Test
        @DisplayName("订单ID为空被 @NotNull 拦截，返回 400")
        void create_missingOrderId() throws Exception {
            mockMvc.perform(post("/api/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"score\":5,\"content\":\"不错\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("重复评价业务异常 5009 以 HTTP 200 返回")
        void create_alreadyRated() throws Exception {
            doThrow(new BusinessException(ErrorCode.ORDER_ALREADY_RATED))
                    .when(reviewService).createReview(eq(ORDER_ID), eq(USER_ID), eq(5), any());

            mockMvc.perform(post("/api/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"orderId\":8,\"score\":5}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_ALREADY_RATED.getCode()));
        }
    }

    @Nested
    @DisplayName("公开查询")
    class QueryTests {

        @Test
        @DisplayName("分页查询用户评价：无需登录，返回分页结构")
        void pageByUser_noAuthRequired() throws Exception {
            PageResult<ReviewVO> page = new PageResult<>();
            page.setRecords(List.of());
            page.setTotal(3L);
            page.setPage(1L);
            page.setPageSize(10L);
            page.setPages(1L);
            when(reviewService.pageByRateeId(3L, 1, 10)).thenReturn(page);

            mockMvc.perform(get("/api/review/user/{userId}", 3L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(3));
        }

        @Test
        @DisplayName("信用汇总返回平均分与好评率")
        void summary_returnsRates() throws Exception {
            ReviewSummaryVO summary = new ReviewSummaryVO();
            summary.setAvgScore(4.8);
            summary.setCount(5L);
            summary.setGoodCount(4L);
            summary.setGoodRate(80.0);
            when(reviewService.summary(3L)).thenReturn(summary);

            mockMvc.perform(get("/api/review/summary/{userId}", 3L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.avgScore").value(4.8))
                    .andExpect(jsonPath("$.data.count").value(5))
                    .andExpect(jsonPath("$.data.goodRate").value(80.0));
        }
    }
}