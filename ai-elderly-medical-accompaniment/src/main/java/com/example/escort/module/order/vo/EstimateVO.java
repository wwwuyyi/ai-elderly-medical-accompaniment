package com.example.escort.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "费用试算结果")
public class EstimateVO {
    private String serviceType;
    private BigDecimal unitPrice;
    private BigDecimal duration;
    private BigDecimal totalAmount;
}