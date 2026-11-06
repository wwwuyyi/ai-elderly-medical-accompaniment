package com.example.escort.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算记录实体
 * 声明：平台抽成比例为模拟规则，非真实商业分账。
 */
@Data
@TableName("settlement")
public class Settlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID（唯一） */
    private Long orderId;

    /** 订单编号（冗余） */
    private String orderNo;

    /** 陪诊师用户ID */
    private Long escortId;

    /** 订单总金额 */
    private BigDecimal orderAmount;

    /** 平台抽成比例 */
    private BigDecimal platformRate;

    /** 平台抽成金额 */
    private BigDecimal platformFee;

    /** 陪诊师收入 */
    private BigDecimal escortIncome;

    /** 结算状态 */
    private String settleStatus;

    /** 操作人ID（管理员） */
    private Long operatorId;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}