package com.campustrade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流
 * <p>
 * 按「登录用户」或「客户端 IP」维度做固定窗口计数，超过阈值直接拒绝，
 * 用于登录爆破、发布刷屏、聊天刷屏这类场景。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许的最大请求数 */
    int limit() default 10;

    /** 窗口长度，单位秒 */
    int windowSeconds() default 60;

    /** 限流维度名，默认取方法名 */
    String key() default "";
}