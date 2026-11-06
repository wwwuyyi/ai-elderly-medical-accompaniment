package com.example.escort.module.order.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 订单号生成工具
 * 规则：OD + yyyyMMdd + 6位序列（当日最大序号+1）
 * 唯一索引兜底 + DuplicateKeyException重试最多3次
 *
 * 兼容MySQL与H2：使用MyBatis-Plus标准查询替代数据库特有SQL函数。
 */
@Component
@RequiredArgsConstructor
public class OrderNoGenerator {

    private final EscortOrderMapper escortOrderMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String today = LocalDate.now().format(DATE_FMT);
        String prefix = "OD" + today;

        // 查询当天订单号前缀匹配的最后一条，按order_no降序取首条
        LambdaQueryWrapper<EscortOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(EscortOrder::getOrderNo, prefix)
                .orderByDesc(EscortOrder::getOrderNo)
                .last("LIMIT 1");
        EscortOrder last = escortOrderMapper.selectOne(wrapper);

        int nextSeq = 1;
        if (last != null) {
            String orderNo = last.getOrderNo();
            // orderNo格式: "OD" + 8位日期 + 6位序列 = 16位总长
            String seqPart = orderNo.substring(10);
            try {
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                // 兼容非预期格式，从1开始
            }
        }
        if (nextSeq > 999999) {
            throw new RuntimeException("当日订单序号已超上限(999999)");
        }
        return prefix + String.format("%06d", nextSeq);
    }
}