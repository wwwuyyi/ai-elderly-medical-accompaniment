package com.example.escort.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "状态流转日志")
public class StatusLogVO {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private String operatorRole;
    private String remark;
    private LocalDateTime createTime;
}