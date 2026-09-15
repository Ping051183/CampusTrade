package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Notice;

import java.util.List;

/**
 * 公告Service接口
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 获取最新公告列表
     */
    List<Notice> getLatestNotices(int limit);
}
