package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campustrade.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统通知Mapper接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}