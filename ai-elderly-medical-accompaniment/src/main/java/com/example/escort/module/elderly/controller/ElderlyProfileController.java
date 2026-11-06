package com.example.escort.module.elderly.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.common.PageResult;
import com.example.escort.common.Result;
import com.example.escort.module.elderly.dto.ElderlyProfileDTO;
import com.example.escort.module.elderly.service.ElderlyProfileService;
import com.example.escort.module.elderly.vo.ElderlyProfileVO;
import com.example.escort.security.RequireRole;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 老人档案控制器
 * /api/elderly（家属）
 */
@Tag(name = "老人档案模块")
@RestController
@RequestMapping("/api/elderly")
@RequireRole("FAMILY")
@RequiredArgsConstructor
public class ElderlyProfileController {

    private final ElderlyProfileService elderlyProfileService;

    // ===== 10. POST /api/elderly 新增老人档案 =====
    @Operation(summary = "新增老人档案")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ElderlyProfileDTO dto) {
        elderlyProfileService.create(UserContext.getUserId(), dto);
        return Result.ok();
    }

    // ===== 11. PUT /api/elderly/{id} 修改老人档案 =====
    @Operation(summary = "修改老人档案")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "档案ID") @PathVariable Long id,
            @Valid @RequestBody ElderlyProfileDTO dto) {
        elderlyProfileService.update(UserContext.getUserId(), id, dto);
        return Result.ok();
    }

    // ===== 12. DELETE /api/elderly/{id} 删除 =====
    @Operation(summary = "删除老人档案")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "档案ID") @PathVariable Long id) {
        elderlyProfileService.delete(UserContext.getUserId(), id);
        return Result.ok();
    }

    // ===== 13. GET /api/elderly/page 分页查询 =====
    @Operation(summary = "分页查询老人档案（仅当前家属名下）")
    @GetMapping("/page")
    public Result<PageResult<ElderlyProfileVO>> page(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        IPage<ElderlyProfileVO> page = elderlyProfileService.page(UserContext.getUserId(), current, size);
        return Result.ok(PageResult.of(page));
    }

    // ===== 14. GET /api/elderly/{id} 详情 =====
    @Operation(summary = "查询老人档案详情")
    @GetMapping("/{id}")
    public Result<ElderlyProfileVO> getById(@Parameter(description = "档案ID") @PathVariable Long id) {
        ElderlyProfileVO vo = elderlyProfileService.getById(UserContext.getUserId(), id);
        return Result.ok(vo);
    }
}