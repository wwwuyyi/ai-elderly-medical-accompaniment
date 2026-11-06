package com.example.escort.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.common.PageResult;
import com.example.escort.common.Result;
import com.example.escort.module.admin.service.AdminService;
import com.example.escort.module.admin.vo.DashboardVO;
import com.example.escort.module.admin.vo.EscortAuditDetailVO;
import com.example.escort.module.auth.entity.SysUser;
import com.example.escort.module.escort.entity.EscortProfile;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.entity.Evaluation;
import com.example.escort.security.RequireRole;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端") @RestController @RequestMapping("/api/admin") @RequireRole("ADMIN") @RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @Operation(summary = "运营看板") @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() { return Result.ok(adminService.dashboard()); }

    @Operation(summary = "资质审核分页") @GetMapping("/escort/audit/page")
    public Result<PageResult<EscortProfile>> auditPage(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String auditStatus) {
        return Result.ok(PageResult.of(adminService.auditPage(current, size, auditStatus))); }

    @Operation(summary = "审核（通过/驳回）") @PostMapping("/escort/audit/{escortUserId}")
    public Result<Void> audit(@PathVariable Long escortUserId, @RequestParam String status, @RequestParam(required = false) String remark) {
        adminService.audit(UserContext.getUserId(), escortUserId, status, remark); return Result.ok(); }

    @Operation(summary = "审核详情（含解密后的身份证号明文，仅ADMIN审核场景）")
    @GetMapping("/escort/audit/{escortUserId}/detail")
    public Result<EscortAuditDetailVO> auditDetail(@PathVariable Long escortUserId) {
        return Result.ok(adminService.getAuditDetail(escortUserId)); }

    @Operation(summary = "用户管理分页") @GetMapping("/user/page")
    public Result<PageResult<SysUser>> userPage(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(adminService.userPage(current, size, keyword))); }

    @Operation(summary = "启用/禁用") @PutMapping("/user/{id}/status")
    public Result<Void> toggleUser(@PathVariable Long id, @RequestParam String status) {
        adminService.toggleUserStatus(id, status); return Result.ok(); }

    @Operation(summary = "全量订单管理") @GetMapping("/order/page")
    public Result<PageResult<EscortOrder>> orderPage(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(adminService.orderPage(current, size, status, keyword))); }

    @Operation(summary = "评价管理分页") @GetMapping("/evaluation/page")
    public Result<PageResult<Evaluation>> evaluationPage(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(PageResult.of(adminService.evaluationPage(current, size))); }

    @Operation(summary = "违规评价隐藏") @PutMapping("/evaluation/{id}/hide")
    public Result<Void> hideEvaluation(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adminService.hideEvaluation(id, reason); return Result.ok(); }
}