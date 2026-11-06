package com.example.escort.module.ai.controller;

import com.example.escort.common.Result;
import com.example.escort.module.ai.dto.ChatSendDTO;
import com.example.escort.module.ai.service.AiChatService;
import com.example.escort.module.ai.vo.ChatMessageVO;
import com.example.escort.module.ai.vo.ChatSessionVO;
import com.example.escort.security.RequireRole;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI智能对话控制器
 * 提供多轮对话会话管理 + SSE流式消息接口
 */
@Tag(name = "AI对话模块")
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @Operation(summary = "创建新会话")
    @PostMapping("/session")
    @RequireRole("FAMILY")
    public Result<ChatSessionVO> createSession(
            @RequestParam(required = false) String contextType,
            @RequestParam(required = false) Long contextId,
            @RequestParam(required = false) String title) {
        return Result.ok(aiChatService.createSession(UserContext.getUserId(), contextType, contextId, title));
    }

    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    @RequireRole("FAMILY")
    public Result<List<ChatSessionVO>> listSessions() {
        return Result.ok(aiChatService.listSessions(UserContext.getUserId()));
    }

    @Operation(summary = "获取会话历史消息")
    @GetMapping("/session/{id}/messages")
    @RequireRole("FAMILY")
    public Result<List<ChatMessageVO>> getMessages(@PathVariable Long id) {
        return Result.ok(aiChatService.getMessages(id, UserContext.getUserId()));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{id}")
    @RequireRole("FAMILY")
    public Result<Void> deleteSession(@PathVariable Long id) {
        aiChatService.deleteSession(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "发送消息（SSE流式返回AI回复）")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireRole("FAMILY")
    public SseEmitter sendMessage(@Valid @RequestBody ChatSendDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2分钟超时
        aiChatService.sendMessage(UserContext.getUserId(), dto, emitter);
        return emitter;
    }
}