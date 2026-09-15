package com.campustrade.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页结果封装类
 */
@Data
public class PageResult<T> {

    /** 数据列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页条数 */
    private long pageSize;

    /** 总页数 */
    private long pages;

    /**
     * 从MyBatis Plus分页对象构建
     */
    public static <T> PageResult<T> from(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.records = page.getRecords();
        result.total = page.getTotal();
        result.page = page.getCurrent();
        result.pageSize = page.getSize();
        result.pages = page.getPages();
        return result;
    }
}
