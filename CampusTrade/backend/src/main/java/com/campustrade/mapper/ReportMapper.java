package com.campustrade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campustrade.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报Mapper接口
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
