package com.example.escort.module.ai.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.escort.common.*;
import com.example.escort.config.AiProperties;
import com.example.escort.module.ai.dto.ChatSendDTO;
import com.example.escort.module.ai.entity.AiChatMessage;
import com.example.escort.module.ai.entity.AiChatSession;
import com.example.escort.module.ai.mapper.AiChatMessageMapper;
import com.example.escort.module.ai.mapper.AiChatSessionMapper;
import com.example.escort.module.ai.service.AiChatService;
import com.example.escort.module.ai.service.ChatContextBuilder;
import com.example.escort.module.ai.vo.ChatMessageVO;
import com.example.escort.module.ai.vo.ChatSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI对话服务实现
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final ChatContextBuilder contextBuilder;
    private final AiHttpClient httpClient;
    private final AiProperties aiProps;
    private final SimpleCircuitBreaker llmBreaker;
    private final ThreadPoolTaskExecutor aiExecutor;

    private static final String DISCLAIMER = "\n\n---\n*AI生成内容仅供参考，不构成任何诊疗建议。*";
    /** 对话历史保留轮数 */
    private static final int MAX_HISTORY_ROUNDS = 10;

    public AiChatServiceImpl(AiChatSessionMapper sm, AiChatMessageMapper mm,
            ChatContextBuilder cb, AiHttpClient hc, AiProperties ap,
            @org.springframework.beans.factory.annotation.Qualifier("llmBreaker") SimpleCircuitBreaker lb,
            @org.springframework.beans.factory.annotation.Qualifier("aiTaskExecutor") ThreadPoolTaskExecutor ex) {
        sessionMapper = sm; messageMapper = mm; contextBuilder = cb;
        httpClient = hc; aiProps = ap; llmBreaker = lb; aiExecutor = ex;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO createSession(Long userId, String contextType, Long contextId, String title) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setContextType(contextType);
        session.setContextId(contextId);
        session.setTitle(title != null ? title : "新对话");
        // 构建系统提示词
        String systemPrompt = contextBuilder.buildSystemPrompt(contextType, contextId);
        session.setSystemPrompt(systemPrompt);
        sessionMapper.insert(session);
        // 写入系统提示词作为首条消息
        AiChatMessage sysMsg = new AiChatMessage();
        sysMsg.setSessionId(session.getId());
        sysMsg.setRole("SYSTEM");
        sysMsg.setContent(systemPrompt);
        messageMapper.insert(sysMsg);
        return toSessionVO(session, null);
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getUpdateTime);
        List<AiChatSession> sessions = sessionMapper.selectList(wrapper);
        return sessions.stream().map(s -> {
            // 获取最后一条非SYSTEM消息作为摘要
            LambdaQueryWrapper<AiChatMessage> msgWrapper = new LambdaQueryWrapper<AiChatMessage>()
                    .eq(AiChatMessage::getSessionId, s.getId())
                    .ne(AiChatMessage::getRole, "SYSTEM")
                    .orderByDesc(AiChatMessage::getCreateTime)
                    .last("LIMIT 1");
            AiChatMessage lastMsg = messageMapper.selectOne(msgWrapper);
            return toSessionVO(s, lastMsg);
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageVO> getMessages(Long sessionId, Long userId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        if (!session.getUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .ne(AiChatMessage::getRole, "SYSTEM")  // 不返回系统消息给前端
                .orderByAsc(AiChatMessage::getCreateTime);
        return messageMapper.selectList(wrapper).stream()
                .map(this::toMessageVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long userId, Long sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        if (!session.getUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        sessionMapper.deleteById(sessionId);
        // 级联删除消息
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId);
        messageMapper.delete(wrapper);
    }

    @Override
    public void sendMessage(Long userId, ChatSendDTO dto, SseEmitter emitter) {
        try {
            // 1. 获取或创建会话
            AiChatSession session;
            if (dto.getSessionId() != null) {
                session = sessionMapper.selectById(dto.getSessionId());
                if (session == null) {
                    emitter.send(SseEmitter.event().data("{\"error\":\"会话不存在\"}"));
                    emitter.complete();
                    return;
                }
                if (!session.getUserId().equals(userId)) {
                    emitter.send(SseEmitter.event().data("{\"error\":\"无权访问该会话\"}"));
                    emitter.complete();
                    return;
                }
            } else {
                // 自动创建会话：标题取用户消息前20字
                String title = dto.getMessage();
                if (title.length() > 20) title = title.substring(0, 20) + "...";
                session = new AiChatSession();
                session.setUserId(userId);
                session.setContextType(dto.getContextType());
                session.setContextId(dto.getContextId());
                session.setTitle(title);
                String systemPrompt = contextBuilder.buildSystemPrompt(dto.getContextType(), dto.getContextId());
                session.setSystemPrompt(systemPrompt);
                sessionMapper.insert(session);
                // 写入系统消息
                AiChatMessage sysMsg = new AiChatMessage();
                sysMsg.setSessionId(session.getId());
                sysMsg.setRole("SYSTEM");
                sysMsg.setContent(systemPrompt);
                messageMapper.insert(sysMsg);
            }

            // 2. 保存用户消息
            AiChatMessage userMsg = new AiChatMessage();
            userMsg.setSessionId(session.getId());
            userMsg.setRole("USER");
            userMsg.setContent(dto.getMessage());
            userMsg.setTokenCount(estimateTokens(dto.getMessage()));
            messageMapper.insert(userMsg);

            // 3. 构建对话上下文（system + 最近N轮历史 + 当前消息）
            List<JSONObject> messages = buildMessagesList(session.getId(), dto.getMessage());

            // 4. 发送初始事件通知前端会话ID
            emitter.send(SseEmitter.event().data("{\"sessionId\":" + session.getId() + "}"));

            // 5. 调用LLM获取回复
            String aiResponse;
            if (!llmBreaker.allowRequest()) {
                aiResponse = "【系统提示】AI服务暂时不可用，请稍后再试。";
            } else {
                try {
                    // DeepSeek API (OpenAI-compatible): POST /chat/completions
                    JSONObject reqBody = new JSONObject();
                    reqBody.set("model", "deepseek-chat");
                    reqBody.set("messages", messages);
                    String respJson = httpClient.llmPost("/chat/completions", reqBody.toString());
                    // Parse OpenAI-compatible response: choices[0].message.content
                    JSONObject respObj = JSONUtil.parseObj(respJson);
                    aiResponse = respObj.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getStr("content");
                    if (aiResponse == null || aiResponse.isBlank()) {
                        aiResponse = "抱歉，我暂时无法回答您的问题，请稍后再试。";
                    }
                    // 长度限制
                    if (aiResponse.length() > 5000) aiResponse = aiResponse.substring(0, 5000) + "...";
                    llmBreaker.onSuccess();
                } catch (Exception ex) {
                    log.error("LLM对话调用失败 sessionId={}", session.getId(), ex);
                    llmBreaker.onFailure();
                    aiResponse = "抱歉，AI服务调用失败。AI生成内容仅供参考，不构成任何诊疗建议。";
                }
            }

            // 6. 拼接免责声明
            String fullResponse = aiResponse + DISCLAIMER;

            // 7. 模拟流式分块推送（按句子拆分，逐句发送）
            int chunkSize = 3; // 每次发送3个字符模拟流式逐字
            for (int i = 0; i < fullResponse.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, fullResponse.length());
                String chunk = fullResponse.substring(i, end);
                emitter.send(SseEmitter.event().data(chunk));
                try { Thread.sleep(30); } catch (InterruptedException ignored) { break; }
            }

            // 8. 发送完成标记
            emitter.send(SseEmitter.event().data("[DONE]"));

            // 9. 保存AI回复到数据库
            AiChatMessage assistantMsg = new AiChatMessage();
            assistantMsg.setSessionId(session.getId());
            assistantMsg.setRole("ASSISTANT");
            assistantMsg.setContent(fullResponse);
            assistantMsg.setTokenCount(estimateTokens(fullResponse));
            messageMapper.insert(assistantMsg);

            // 10. 更新会话时间
            sessionMapper.updateById(session);

            emitter.complete();

        } catch (Exception e) {
            log.error("发送消息异常 userId={}", userId, e);
            try {
                emitter.send(SseEmitter.event().data("{\"error\":\"系统异常，请重试\"}"));
            } catch (IOException ignored) {}
            emitter.completeWithError(e);
        }
    }

    /**
     * 构建LLM请求的消息列表（system + 最近历史 + 当前消息）
     */
    private List<JSONObject> buildMessagesList(Long sessionId, String currentMessage) {
        List<JSONObject> messages = new ArrayList<>();

        // 获取会话的system prompt
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session != null && session.getSystemPrompt() != null) {
            JSONObject sysMsg = new JSONObject();
            sysMsg.set("role", "system");
            sysMsg.set("content", session.getSystemPrompt());
            messages.add(sysMsg);
        }

        // 获取最近N轮历史消息（不含SYSTEM）
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .ne(AiChatMessage::getRole, "SYSTEM")
                .orderByDesc(AiChatMessage::getCreateTime)
                .last("LIMIT " + (MAX_HISTORY_ROUNDS * 2)); // N轮 = N*2条
        List<AiChatMessage> history = messageMapper.selectList(wrapper);
        // 反转回正序
        java.util.Collections.reverse(history);
        for (AiChatMessage msg : history) {
            JSONObject m = new JSONObject();
            m.set("role", msg.getRole().toLowerCase());
            m.set("content", msg.getContent());
            messages.add(m);
        }

        // 添加当前用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", currentMessage);
        messages.add(userMsg);

        return messages;
    }

    /** 估算token数（中文约1.5字符/token） */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        return (int) Math.ceil(text.length() / 1.5);
    }

    private ChatSessionVO toSessionVO(AiChatSession s, AiChatMessage lastMsg) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setContextType(s.getContextType());
        vo.setUpdateTime(s.getUpdateTime());
        if (lastMsg != null) {
            String content = lastMsg.getContent();
            vo.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        }
        // 消息计数
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<AiChatMessage>().eq(AiChatMessage::getSessionId, s.getId()));
        vo.setMessageCount(count != null ? count.intValue() : 0);
        return vo;
    }

    private ChatMessageVO toMessageVO(AiChatMessage m) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(m.getId());
        vo.setSessionId(m.getSessionId());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setTokenCount(m.getTokenCount());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}