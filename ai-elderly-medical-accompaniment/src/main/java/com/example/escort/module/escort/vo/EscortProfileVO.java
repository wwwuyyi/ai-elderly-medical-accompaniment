package com.example.escort.module.escort.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 陪诊师信息响应VO
 */
@Data
@Schema(description = "陪诊师信息")
public class EscortProfileVO {

    @Schema(description = "信息ID")
    private Long id;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private String birthDate;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "服务城市")
    private String serviceCity;

    @Schema(description = "服务区域")
    private String serviceArea;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "审核状态：PENDING/APPROVED/REJECTED")
    private String auditStatus;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "平均评分")
    private BigDecimal avgRating;

    @Schema(description = "累计订单数")
    private Integer totalOrders;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}