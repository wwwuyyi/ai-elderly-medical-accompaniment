package com.example.escort.module.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Schema(description = "OCR任务结果")
public class OcrTaskVO {
    private Long taskId;
    private String status;
    private String ocrResult;
    private BigDecimal ocrConfidence;
    private Integer manualConfirmed;
    private String errorMsg;
    private LocalDateTime createTime;
}