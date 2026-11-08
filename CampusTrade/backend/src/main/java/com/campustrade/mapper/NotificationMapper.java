package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campustrade.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统通知Mapper接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 标记单条通知已读（条件带 user_id，防止越权修改他人通知）
     */
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 标记某用户全部通知已读
     */
    int markAllRead(@Param("userId") Long userId);
}