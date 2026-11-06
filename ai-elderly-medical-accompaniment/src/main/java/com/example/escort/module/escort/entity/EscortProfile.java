package com.example.escort.module.escort.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 陪诊师信息实体
 */
@Data
@TableName("escort_profile")
public class EscortProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID（一对一） */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 性别：MALE / FEMALE */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 身份证号（AES密文存储） */
    private String idCard;

    /** 联系电话 */
    private String phone;

    /** 服务城市 */
    private String serviceCity;

    /** 服务区域 */
    private String serviceArea;

    /** 个人简介 */
    private String intro;

    /** 资质材料图片JSON数组 */
    private String materials;

    /** 审核状态：PENDING / APPROVED / REJECTED */
    private String auditStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核人ID */
    private Long auditorId;

    /** 平均评分 */
    private BigDecimal avgRating;

    /** 累计订单数 */
    private Integer totalOrders;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}