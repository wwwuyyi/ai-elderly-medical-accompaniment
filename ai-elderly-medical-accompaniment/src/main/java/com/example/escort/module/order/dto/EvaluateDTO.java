package com.example.escort.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "服务评价请求")
public class EvaluateDTO {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    @Schema(description = "评分（1-5星）")
    private Integer score;
    @Schema(description = "评价内容")
    private String content;
}