package com.campustrade.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置类
 * 配置分页插件与慢 SQL 拦截
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis Plus拦截器（分页插件）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 慢 SQL 拦截器：被 MyBatis-Plus 自动装配，统计单条 SQL 耗时
     */
    @Bean
    public SlowSqlInterceptor slowSqlInterceptor(
            @Value("${campustrade.slow-sql.threshold-ms:500}") long thresholdMs) {
        return new SlowSqlInterceptor(thresholdMs);
    }
}
