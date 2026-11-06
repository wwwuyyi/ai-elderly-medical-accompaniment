package com.example.escort.module.ai.service;

import com.example.escort.module.ai.dto.ChatSendDTO;
import com.example.escort.module.ai.vo.ChatMessageVO;
import com.example.escort.module.ai.vo.ChatSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI对话服务接口
 */
public interface AiChatService {

    /** 创建会话（含system prompt初始化） */
    ChatSessionVO createSession(Long userId, String contextType, Long contextId, String title);

    /** 查询用户会话列表（按更新时间倒序） */
    List<ChatSessionVO> listSessions(Long userId);

    /** 查询会话历史消息（按时间正序） */
    List<ChatMessageVO> getMessages(Long sessionId, Long userId);

    /** 删除会话 */
    void deleteSession(Long userId, Long sessionId);

    /** 发送消息并通过SseEmitter流式返回AI回复 */
    void sendMessage(Long userId, ChatSendDTO dto, SseEmitter emitter);
}