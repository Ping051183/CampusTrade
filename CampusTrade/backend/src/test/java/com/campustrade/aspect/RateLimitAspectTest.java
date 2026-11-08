package com.campustrade.aspect;

import com.campustrade.annotation.RateLimit;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.infra.RedisGuard;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitAspect 单元测试：阈值内放行、超阈值拒绝。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("限流切面")
class RateLimitAspectTest {

    @Mock
    private RedisGuard redisGuard;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private RateLimit rateLimit;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(redisGuard);
        lenient().when(rateLimit.limit()).thenReturn(5);
        lenient().when(rateLimit.windowSeconds()).thenReturn(60);
        lenient().when(rateLimit.key()).thenReturn("login");
    }

    @Test
    @DisplayName("窗口内未超阈值时正常执行业务")
    void passWhenUnderLimit() throws Throwable {
        when(redisGuard.increment(startsWith("rate:login:"), any(Duration.class))).thenReturn(3L);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, rateLimit);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("超过阈值时抛限流异常且不执行业务")
    void rejectWhenOverLimit() throws Throwable {
        when(redisGuard.increment(anyString(), any(Duration.class))).thenReturn(6L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, rateLimit));

        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(), ex.getCode());
        verify(joinPoint, never()).proceed();
    }
}