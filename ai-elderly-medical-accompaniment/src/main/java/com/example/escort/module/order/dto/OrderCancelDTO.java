package com.example.escort.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "取消订单请求")
public class OrderCancelDTO {
    @Schema(description = "取消原因（T10必须填写）")
    private String cancelReason;
}