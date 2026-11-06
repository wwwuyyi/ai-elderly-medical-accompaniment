package com.example.escort.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页统一返回体
 * @param <T> 记录类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页码 */
    private long current;
    /** 每页大小 */
    private long size;

    /**
     * 从 MyBatis-Plus IPage 转换
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 手动构建
     */
    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records, total, current, size);
    }
}