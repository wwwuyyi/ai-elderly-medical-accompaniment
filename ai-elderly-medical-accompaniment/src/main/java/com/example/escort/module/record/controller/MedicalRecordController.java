package com.example.escort.module.record.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.common.PageResult;
import com.example.escort.common.Result;
import com.example.escort.module.record.dto.MedicalRecordDTO;
import com.example.escort.module.record.service.MedicalRecordService;
import com.example.escort.module.record.vo.MedicalDocumentVO;
import com.example.escort.module.record.vo.MedicalRecordVO;
import com.example.escort.security.RequireRole;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "就医档案模块")
@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor
public class MedicalRecordController {
    private final MedicalRecordService recordService;

    @Operation(summary = "创建就医档案")
    @PostMapping @RequireRole("FAMILY")
    public Result<MedicalRecordVO> create(@Valid @RequestBody MedicalRecordDTO dto) {
        return Result.ok(recordService.create(UserContext.getUserId(), UserContext.getUserRole(), dto));
    }

    @Operation(summary = "修改就医档案")
    @PutMapping("/{id}") @RequireRole("FAMILY")
    public Result<MedicalRecordVO> update(@PathVariable Long id, @Valid @RequestBody MedicalRecordDTO dto) {
        return Result.ok(recordService.update(UserContext.getUserId(), UserContext.getUserRole(), id, dto));
    }

    @Operation(summary = "分页查询（按角色过滤）")
    @GetMapping("/page")
    public Result<PageResult<MedicalRecordVO>> page(@RequestParam(required = false) Long elderlyId,
            @RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "10") int size) {
        IPage<MedicalRecordVO> p = recordService.page(UserContext.getUserId(), UserContext.getUserRole(), elderlyId, current, size);
        return Result.ok(PageResult.of(p));
    }

    @Operation(summary = "详情（按角色脱敏）")
    @GetMapping("/{id}")
    public Result<MedicalRecordVO> detail(@PathVariable Long id) {
        return Result.ok(recordService.getById(UserContext.getUserId(), UserContext.getUserRole(), id));
    }

    @Operation(summary = "删除就医档案")
    @DeleteMapping("/{id}") @RequireRole("FAMILY")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.delete(UserContext.getUserId(), UserContext.getUserRole(), id);
        return Result.ok();
    }

    @Operation(summary = "上传医疗单据")
    @PostMapping("/{id}/document") @RequireRole("ESCORT")
    public Result<MedicalDocumentVO> uploadDoc(@PathVariable Long id,
            @RequestParam String docType, @RequestParam String imageUrl) {
        return Result.ok(recordService.uploadDocument(UserContext.getUserId(), UserContext.getUserRole(), id, docType, imageUrl));
    }

    @Operation(summary = "单据列表（需可见性校验）")
    @GetMapping("/{id}/documents")
    public Result<List<MedicalDocumentVO>> documents(@PathVariable Long id) {
        return Result.ok(recordService.getDocuments(UserContext.getUserId(), UserContext.getUserRole(), id));
    }

    @Operation(summary = "删除单据")
    @DeleteMapping("/document/{docId}") @RequireRole("FAMILY")
    public Result<Void> deleteDoc(@PathVariable Long docId) {
        recordService.deleteDocument(UserContext.getUserId(), UserContext.getUserRole(), docId);
        return Result.ok();
    }
}