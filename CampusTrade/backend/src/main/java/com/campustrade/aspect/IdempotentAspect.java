package com.campustrade.aspect;

import com.campustrade.annotation.Idempotent;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.infra.RedisGuard;
import com.campustrade.security.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;

/**
 * 幂等切面
 * <p>
 * 同一主体（用户 / IP）+ 同一方法 + 同一幂等键，在窗口内只真正执行一次业务；
 * 重复请求直接回放首次结果，既不会重复下单，也不会让用户看到一条莫名其妙的报错。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String HEADER = "Idempotency-Key";

    private final RedisGuard redisGuard;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String requestKey = resolveKey(joinPoint, idempotent);
        String fingerprint = "idem:" + subject() + ":" + methodName(joinPoint) + ":" + requestKey;
        String resultKey = fingerprint + ":result";

        String cached = redisGuard.get(resultKey);
        if (cached != null) {
            log.info("命中幂等缓存，回放首次结果：{}", fingerprint);
            return deserialize(cached, joinPoint);
        }

        boolean acquired = redisGuard.setIfAbsent(fingerprint + ":lock", "1",
                Duration.ofSeconds(idempotent.ttlSeconds()));
        if (!acquired) {
            String again = redisGuard.get(resultKey);
            if (again != null) {
                return deserialize(again, joinPoint);
            }
            throw new BusinessException(ErrorCode.IDEMPOTENT_CONFLICT);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 业务失败要释放锁，否则用户重试会被自己的失败请求挡住一个窗口
            redisGuard.delete(fingerprint + ":lock");
            throw e;
        }
        try {
            redisGuard.set(resultKey, objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(idempotent.ttlSeconds()));
        } catch (Exception e) {
            log.warn("幂等结果缓存失败，不影响本次响应：{}", e.getMessage());
        }
        return result;
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        HttpServletRequest request = currentRequest();
        String header = request == null ? null : request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        if (idempotent.requireHeader()) {
            throw new BusinessException(ErrorCode.IDEMPOTENT_KEY_REQUIRED);
        }
        // 未携带幂等键时用入参指纹兜底，前端不改也能拦住"连点两下"
        return "auto:" + Arrays.deepToString(joinPoint.getArgs()).hashCode();
    }

    private Object deserialize(String json, ProceedingJoinPoint joinPoint) {
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Type type = method.getGenericReturnType();
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
            log.warn("幂等结果反序列化失败，改为拒绝重复提交：{}", e.getMessage());
            throw new BusinessException(ErrorCode.IDEMPOTENT_CONFLICT);
        }
    }

    private String methodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private String subject() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId != null) {
            return "u" + userId;
        }
        HttpServletRequest request = currentRequest();
        return "ip" + (request == null ? "unknown" : clientIp(request));
    }

    static HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes
                ? servletAttributes.getRequest() : null;
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}