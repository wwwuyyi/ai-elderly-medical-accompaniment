package com.example.escort.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 陪诊订单实体
 */
@Data
@TableName("escort_order")
public class EscortOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号（OD+yyyyMMdd+6位序列） */
    private String orderNo;

    /** 家属用户ID */
    private Long familyId;

    /** 老人档案ID */
    private Long elderlyId;

    /** 陪诊师用户ID（T2派单后写入） */
    private Long escortId;

    /** 医院ID */
    private Long hospitalId;

    /** 就诊科室 */
    private String department;

    /** 服务类型：BASIC/EXAM */
    private String serviceType;

    /** 预约就诊开始时间 */
    private LocalDateTime appointmentTime;

    /** 服务时长（小时） */
    private BigDecimal duration;

    /** 服务金额（元） */
    private BigDecimal amount;

    /** 订单状态 */
    private String status;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 支付状态：UNPAID/PAID/PARTIAL_REFUNDED/REFUNDED */
    private String payStatus;

    /** 支付时间（T1下单时记录） */
    private LocalDateTime payTime;

    /** 取消原因 */
    private String cancelReason;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 备注 */
    private String remark;

    /** 幂等请求号（客户端生成，唯一索引兜底） */
    private String requestId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}