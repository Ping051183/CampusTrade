package com.campustrade.aspect;

import com.campustrade.annotation.Idempotent;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.infra.RedisGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IdempotentAspect 单元测试：回放、执行并缓存、并发冲突、失败释放锁。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("幂等切面")
class IdempotentAspectTest {

    /** 用作被拦截方法的宿主 */
    public static class SampleController {
        public String create() {
            return "real-result";
        }
    }

    @Mock
    private RedisGuard redisGuard;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature signature;
    @Mock
    private Idempotent idempotent;

    private IdempotentAspect aspect;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new IdempotentAspect(redisGuard, new ObjectMapper());
        Method method = SampleController.class.getMethod("create");
        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(signature.getName()).thenReturn("create");
        lenient().when(signature.getDeclaringType()).thenReturn(SampleController.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[0]);
        lenient().when(idempotent.ttlSeconds()).thenReturn(60);
        lenient().when(idempotent.requireHeader()).thenReturn(false);
    }

    @Test
    @DisplayName("命中缓存时回放首次结果，不再执行业务")
    void replayCachedResult() throws Throwable {
        when(redisGuard.get(anyString())).thenReturn("\"cached-result\"");

        Object result = aspect.around(joinPoint, idempotent);

        assertEquals("cached-result", result);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("首次请求执行成功后写入结果缓存")
    void executeAndCache() throws Throwable {
        when(redisGuard.get(anyString())).thenReturn(null);
        when(redisGuard.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("real-result");

        Object result = aspect.around(joinPoint, idempotent);

        assertEquals("real-result", result);
        verify(redisGuard).set(startsWith("idem:"), contains("real-result"), any(Duration.class));
    }

    @Test
    @DisplayName("并发重复请求且尚无结果时返回幂等冲突")
    void conflictWhenConcurrent() throws Throwable {
        when(redisGuard.get(anyString())).thenReturn(null);
        when(redisGuard.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, idempotent));

        assertEquals(ErrorCode.IDEMPOTENT_CONFLICT.getCode(), ex.getCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("业务异常时释放幂等锁，允许用户重试")
    void releaseLockOnFailure() {
        when(redisGuard.get(anyString())).thenReturn(null);
        when(redisGuard.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        try {
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        } catch (Throwable ignored) {
            // 桩方法声明了 throws，这里吞掉
        }

        assertThrows(IllegalStateException.class, () -> aspect.around(joinPoint, idempotent));
        verify(redisGuard).delete(endsWith(":lock"));
    }
}