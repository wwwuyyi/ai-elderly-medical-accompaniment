package com.example.escort.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送对话消息请求
 */
@Data
public class ChatSendDTO {
    /** 会话ID（为null时自动创建新会话） */
    private Long sessionId;
    /** 上下文类型：ELDERLY/ORDER/RECORD/GENERAL */
    private String contextType;
    /** 上下文关联业务ID */
    private Long contextId;
    /** 用户消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String message;
}