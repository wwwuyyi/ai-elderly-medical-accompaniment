package com.example.escort.module.ai.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话消息视图
 */
@Data
public class ChatMessageVO {
    private Long id;
    /** 所属会话ID */
    private Long sessionId;
    /** 消息角色：USER/ASSISTANT/SYSTEM */
    private String role;
    /** 消息内容 */
    private String content;
    /** token数量估算 */
    private Integer tokenCount;
    /** 消息时间 */
    private LocalDateTime createTime;
}