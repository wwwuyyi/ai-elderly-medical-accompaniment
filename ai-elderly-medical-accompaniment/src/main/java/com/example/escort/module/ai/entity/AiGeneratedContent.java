package com.example.escort.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_generated_content")
public class AiGeneratedContent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String contentType;
    private Long bizId;
    private String content;
    private String disclaimer;
    private String reviewStatus;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String reviewRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}