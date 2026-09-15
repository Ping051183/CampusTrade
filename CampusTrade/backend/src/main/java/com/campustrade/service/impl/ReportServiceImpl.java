package com.campustrade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Report;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.ReportMapper;
import com.campustrade.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 举报Service实现类
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    private final ReportMapper reportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReport(Report report) {
        report.setStatus(0); // 待处理
        reportMapper.insert(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long reportId, Integer status, String result, Long handlerId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        report.setStatus(status);
        report.setHandleResult(result);
        report.setHandlerId(handlerId);
        reportMapper.updateById(report);
    }
}
