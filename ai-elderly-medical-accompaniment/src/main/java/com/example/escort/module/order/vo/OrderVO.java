package com.example.escort.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单信息")
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long familyId;
    private String elderlyName;
    private Long elderlyId;
    private Long escortId;
    private String escortName;
    private String hospitalName;
    private String department;
    private String serviceType;
    private LocalDateTime appointmentTime;
    private BigDecimal duration;
    private BigDecimal amount;
    private String status;
    private String payStatus;
    private BigDecimal refundAmount;
    private String cancelReason;
    private LocalDateTime createTime;
}