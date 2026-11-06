package com.example.escort.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务评价实体
 */
@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID（唯一） */
    private Long orderId;

    /** 订单编号（冗余） */
    private String orderNo;

    /** 评价人家属ID */
    private Long familyId;

    /** 被评价陪诊师ID */
    private Long escortId;

    /** 评分（1-5） */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 是否隐藏 */
    private Integer isHidden;

    /** 隐藏原因 */
    private String hideReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}