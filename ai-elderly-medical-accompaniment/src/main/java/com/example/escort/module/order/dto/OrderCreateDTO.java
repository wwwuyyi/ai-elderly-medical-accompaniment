package com.example.escort.module.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单请求DTO
 * 含幂等请求号（客户端生成，防重复提交）
 */
@Data
@Schema(description = "下单请求")
public class OrderCreateDTO {

    @NotBlank(message = "幂等请求号不能为空")
    @Schema(description = "客户端请求号（幂等防重，同一请求号只处理一次）", example = "REQ20261027120000")
    private String requestId;

    @NotNull(message = "老人档案ID不能为空")
    @Schema(description = "老人档案ID")
    private Long elderlyId;

    @NotNull(message = "医院ID不能为空")
    @Schema(description = "就诊医院ID")
    private Long hospitalId;

    @NotBlank(message = "就诊科室不能为空")
    @Schema(description = "就诊科室")
    private String department;

    @NotBlank(message = "服务类型不能为空")
    @Schema(description = "服务类型：BASIC/EXAM")
    private String serviceType;

    @NotNull(message = "预约时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "预约就诊时间", example = "2026-11-01 09:00:00")
    private LocalDateTime appointmentTime;

    @NotNull(message = "服务时长不能为空")
    @Schema(description = "服务时长（小时）")
    private BigDecimal duration;

    @NotNull(message = "服务金额不能为空")
    @Schema(description = "确认金额（须与试算结果一致）")
    private BigDecimal amount;
}