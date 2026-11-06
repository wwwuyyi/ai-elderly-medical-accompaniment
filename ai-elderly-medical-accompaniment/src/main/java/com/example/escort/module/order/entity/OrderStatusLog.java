package com.example.escort.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态流转日志实体
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单编号（冗余） */
    private String orderNo;

    /** 变更前状态（首次为NULL） */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人角色 */
    private String operatorRole;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}