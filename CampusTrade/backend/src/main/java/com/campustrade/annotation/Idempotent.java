package com.campustrade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等提交保护
 * <p>
 * 标注在写接口上，同一用户（未登录则按 IP）+ 同一方法 + Idempotency-Key 在 ttl 内只执行一次，
 * 重复请求直接返回首次结果，而不是再走一遍业务（避免重复下单、重复扣款）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等窗口，单位秒 */
    int ttlSeconds() default 60;

    /** 是否强制要求请求头带 Idempotency-Key */
    boolean requireHeader() default false;
}