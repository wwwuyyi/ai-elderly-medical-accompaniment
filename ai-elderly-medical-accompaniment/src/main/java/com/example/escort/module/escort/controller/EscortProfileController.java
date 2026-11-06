package com.example.escort.module.escort.controller;

import com.example.escort.common.Result;
import com.example.escort.module.escort.dto.EscortApplyDTO;
import com.example.escort.module.escort.dto.EscortProfileDTO;
import com.example.escort.module.escort.service.EscortProfileService;
import com.example.escort.module.escort.vo.EscortProfileVO;
import com.example.escort.security.RequireRole;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 陪诊师控制器
 * /api/escort
 */
@Tag(name = "陪诊师模块")
@RestController
@RequestMapping("/api/escort")
@RequireRole("ESCORT")
@RequiredArgsConstructor
public class EscortProfileController {

    private final EscortProfileService escortProfileService;

    // ===== 15. POST /api/escort/apply 提交资质认证 =====
    @Operation(summary = "提交资质认证申请")
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody EscortApplyDTO dto) {
        escortProfileService.apply(UserContext.getUserId(), dto);
        return Result.ok();
    }

    // ===== 16. GET /api/escort/apply/status 查询审核状态 =====
    @Operation(summary = "查询本人资质审核状态")
    @GetMapping("/apply/status")
    public Result<EscortProfileVO> applyStatus() {
        EscortProfileVO vo = escortProfileService.getApplyStatus(UserContext.getUserId());
        return Result.ok(vo);
    }

    // ===== 17. GET /api/escort/profile 查询服务资料 =====
    @Operation(summary = "查询陪诊师服务资料")
    @GetMapping("/profile")
    public Result<EscortProfileVO> getProfile() {
        EscortProfileVO vo = escortProfileService.getProfile(UserContext.getUserId());
        return Result.ok(vo);
    }

    // ===== 18. PUT /api/escort/profile 修改服务资料 =====
    @Operation(summary = "修改陪诊师服务资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody EscortProfileDTO dto) {
        escortProfileService.updateProfile(UserContext.getUserId(), dto);
        return Result.ok();
    }

    // ===== 19. GET /api/escort/order/pool =====
    @Operation(summary = "可接订单池")
    @GetMapping("/order/pool")
    public Result<?> orderPool() { return Result.ok(escortProfileService.getOrderPool(UserContext.getUserId())); }

    // ===== 20. GET /api/escort/statistics =====
    @Operation(summary = "我的统计")
    @GetMapping("/statistics")
    public Result<?> statistics() { return Result.ok(escortProfileService.getStatistics(UserContext.getUserId())); }
}