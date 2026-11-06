package com.example.escort.module.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Schema(description = "AI生成内容")
public class ContentVO {
    private Long id;
    private String contentType;
    private String content;
    private String disclaimer;
    private String reviewStatus;
    private String reviewRemark;
    private LocalDateTime createTime;
}