package com.campustrade.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 慢 SQL 拦截器
 * <p>
 * 统计每条 SQL（query / update）的执行耗时，超过阈值打 WARN 日志，带上 Mapper 方法与 traceId，
 * 便于定位"接口慢"到底是慢在数据库还是慢在业务代码。
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SlowSqlInterceptor implements Interceptor {

    private final long thresholdMs;

    public SlowSqlInterceptor(long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            if (costMs >= thresholdMs) {
                MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
                log.warn("[慢SQL] {} 耗时 {}ms（阈值 {}ms）", ms.getId(), costMs, thresholdMs);
            }
        }
    }
}