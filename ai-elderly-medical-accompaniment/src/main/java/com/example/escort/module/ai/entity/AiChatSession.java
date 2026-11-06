package com.example.escort.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI对话会话实体
 */
@Data
@TableName("ai_chat_session")
public class AiChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会话所属用户ID */
    private Long userId;
    /** 会话标题（首条消息前20字自动生成） */
    private String title;
    /** 上下文类型：ELDERLY/ORDER/RECORD/GENERAL */
    private String contextType;
    /** 上下文关联业务ID */
    private Long contextId;
    /** 注入的系统提示词 */
    private String systemPrompt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}