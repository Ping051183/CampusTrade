package com.campustrade.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TraceIdFilter 单元测试：生成、透传、清理。
 */
@DisplayName("链路追踪过滤器")
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("没有传入 traceId 时生成 16 位并写入响应头与 MDC")
    void generateWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/goods/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] duringRequest = new String[1];
        FilterChain chain = (req, res) -> duringRequest[0] = MDC.get("traceId");

        filter.doFilter(request, response, chain);

        String traceId = response.getHeader("X-Trace-Id");
        assertEquals(16, traceId.length());
        assertEquals(traceId, duringRequest[0]);
        assertNull(MDC.get("traceId"), "请求结束后必须清理 MDC，避免线程复用串号");
    }

    @Test
    @DisplayName("调用方传入合法 traceId 时原样复用，实现跨服务串联")
    void reuseIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/goods/list");
        request.addHeader("X-Trace-Id", "trace-abc-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals("trace-abc-001", response.getHeader("X-Trace-Id"));
    }

    @Test
    @DisplayName("非法 traceId 被替换，防止日志注入")
    void rejectIllegalTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/goods/list");
        request.addHeader("X-Trace-Id", "bad id\nwith newline");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String traceId = response.getHeader("X-Trace-Id");
        assertNotEquals("bad id\nwith newline", traceId);
        assertTrue(traceId.matches("[A-Za-z0-9_-]{16}"));
    }
}