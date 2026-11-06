package com.example.escort.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI对话消息实体
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属会话ID */
    private Long sessionId;
    /** 消息角色：USER/ASSISTANT/SYSTEM */
    private String role;
    /** 消息内容 */
    private String content;
    /** token数量估算 */
    private Integer tokenCount;
    private LocalDateTime createTime;
}