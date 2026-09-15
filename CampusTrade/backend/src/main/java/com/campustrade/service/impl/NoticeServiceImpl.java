package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Notice;
import com.campustrade.mapper.NoticeMapper;
import com.campustrade.service.NoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告Service实现类
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public List<Notice> getLatestNotices(int limit) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Notice::getCreateTime)
                .last("LIMIT " + limit);
        return baseMapper.selectList(wrapper);
    }
}
