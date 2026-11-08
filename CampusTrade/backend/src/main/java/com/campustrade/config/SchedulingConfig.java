package com.campustrade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * <p>
 * 开启 @Scheduled 支持（订单超时自动取消等后台任务）。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}