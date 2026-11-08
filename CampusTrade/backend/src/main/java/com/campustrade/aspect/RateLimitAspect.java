package com.campustrade.aspect;

import com.campustrade.annotation.RateLimit;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.infra.RedisGuard;
import com.campustrade.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 限流切面：固定窗口计数
 * <p>
 * 已登录按用户 id 计数，未登录（登录/注册接口）按客户端 IP 计数。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisGuard redisGuard;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String dimension = rateLimit.key().isBlank()
                ? ((MethodSignature) joinPoint.getSignature()).getMethod().getName()
                : rateLimit.key();
        String key = "rate:" + dimension + ":" + subject();

        long count = redisGuard.increment(key, Duration.ofSeconds(rateLimit.windowSeconds()));
        if (count > rateLimit.limit()) {
            log.warn("限流触发：{} 第 {} 次请求，阈值 {}/{}s", key, count,
                    rateLimit.limit(), rateLimit.windowSeconds());
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        return joinPoint.proceed();
    }

    private String subject() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId != null) {
            return "u" + userId;
        }
        HttpServletRequest request = IdempotentAspect.currentRequest();
        return "ip" + (request == null ? "unknown" : IdempotentAspect.clientIp(request));
    }
}