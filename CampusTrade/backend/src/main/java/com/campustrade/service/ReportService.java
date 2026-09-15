package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Report;

/**
 * 举报Service接口
 */
public interface ReportService extends IService<Report> {

    /**
     * 举报商品
     */
    void addReport(Report report);

    /**
     * 处理举报
     */
    void handleReport(Long reportId, Integer status, String result, Long handlerId);
}
