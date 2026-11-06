package com.example.escort.module.ai.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话会话列表项视图
 */
@Data
public class ChatSessionVO {
    private Long id;
    /** 会话标题 */
    private String title;
    /** 上下文类型 */
    private String contextType;
    /** 最后一条消息摘要 */
    private String lastMessage;
    /** 消息总数 */
    private Integer messageCount;
    /** 最后更新时间 */
    private LocalDateTime updateTime;
}