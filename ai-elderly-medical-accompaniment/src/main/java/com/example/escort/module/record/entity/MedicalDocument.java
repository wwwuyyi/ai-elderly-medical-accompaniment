package com.example.escort.module.record.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medical_document")
public class MedicalDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private String docType;
    private String imageUrl;
    private String ocrStatus;
    private String ocrResult;
    private BigDecimal ocrConfidence;
    private Integer manualConfirmed;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}