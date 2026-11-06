package com.example.escort.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 陪诊师资质审核详情VO
 * 包含解密后的身份证号明文，仅限ADMIN审核场景使用。
 * 注意：该VO不与通用脱敏序列化器耦合，不可复用至其他场景。
 */
@Data
@Schema(description = "陪诊师审核详情（含身份证号明文）")
public class EscortAuditDetailVO {
    private Long id;
    private Long userId;
    private String realName;
    private String gender;
    private LocalDate birthDate;
    @Schema(description = "身份证号（审核场景解密为明文）")
    private String idCard;
    private String phone;
    private String serviceCity;
    private String serviceArea;
    private String intro;
    private String materials;
    private String auditStatus;
    private String auditRemark;
    private LocalDateTime auditTime;
    private Long auditorId;
    private BigDecimal avgRating;
    private Integer totalOrders;
    private LocalDateTime createTime;
}