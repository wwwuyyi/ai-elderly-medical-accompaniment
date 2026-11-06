package com.example.escort.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_task")
public class AiTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private Long bizId;
    private String status;
    private Integer retryCount;
    private String errorMsg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer costMs;
    private String requestDigest;
    private String responseDigest;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}