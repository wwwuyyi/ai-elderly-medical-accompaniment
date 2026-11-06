package com.example.escort.module.record.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "医疗单据")
public class MedicalDocumentVO {
    private Long id;
    private Long recordId;
    private String docType;
    private String imageUrl;
    private String ocrStatus;
    private String ocrResult;
    private BigDecimal ocrConfidence;
    private Integer manualConfirmed;
    private LocalDateTime createTime;
}