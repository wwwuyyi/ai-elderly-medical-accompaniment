package com.example.escort.module.ai.controller;

import com.example.escort.common.Result;
import com.example.escort.module.ai.dto.*;
import com.example.escort.module.ai.service.AiService;
import com.example.escort.module.ai.vo.ContentVO;
import com.example.escort.module.ai.vo.OcrTaskVO;
import com.example.escort.security.RequireRole;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI服务模块")
@RestController @RequestMapping("/api/ai") @RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

    @Operation(summary = "提交OCR识别（异步）")
    @PostMapping("/ocr/recognize") @RequireRole("ESCORT")
    public Result<OcrTaskVO> submitOcr(@Valid @RequestBody OcrSubmitDTO dto) {
        return Result.ok(aiService.submitOcr(UserContext.getUserId(), dto)); }

    @Operation(summary = "轮询OCR结果")
    @GetMapping("/ocr/task/{taskId}") @RequireRole("ESCORT")
    public Result<OcrTaskVO> pollOcr(@PathVariable Long taskId) {
        return Result.ok(aiService.pollOcrTask(taskId)); }

    @Operation(summary = "确认OCR（可修正）")
    @PostMapping("/ocr/task/{taskId}/confirm") @RequireRole("ESCORT")
    public Result<Void> confirmOcr(@PathVariable Long taskId, @RequestBody OcrConfirmDTO dto) {
        aiService.confirmOcr(UserContext.getUserId(), taskId, dto); return Result.ok(); }

    @Operation(summary = "生成就诊告知书")
    @PostMapping("/notice/generate") @RequireRole("FAMILY")
    public Result<ContentVO> generateNotice(@Valid @RequestBody NoticeGenerateDTO dto) {
        return Result.ok(aiService.generateNotice(UserContext.getUserId(), dto)); }

    @Operation(summary = "生成医嘱解读")
    @PostMapping("/explain/generate") @RequireRole("FAMILY")
    public Result<ContentVO> generateExplain(@Valid @RequestBody ExplainGenerateDTO dto) {
        return Result.ok(aiService.generateExplain(UserContext.getUserId(), dto)); }

    @Operation(summary = "查看AI生成内容")
    @GetMapping("/content/{id}")
    public Result<ContentVO> getContent(@PathVariable Long id) {
        return Result.ok(aiService.getContent(id)); }

    @Operation(summary = "人工复核")
    @PutMapping("/content/{id}/review") @RequireRole("ADMIN")
    public Result<Void> reviewContent(@PathVariable Long id, @Valid @RequestBody ContentReviewDTO dto) {
        aiService.reviewContent(UserContext.getUserId(), id, dto); return Result.ok(); }
}