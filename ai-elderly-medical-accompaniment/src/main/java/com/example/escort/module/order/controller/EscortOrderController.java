package com.example.escort.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.common.PageResult;
import com.example.escort.common.Result;
import com.example.escort.module.order.dto.*;
import com.example.escort.module.order.service.OrderService;
import com.example.escort.module.order.vo.EstimateVO;
import com.example.escort.module.order.vo.OrderVO;
import com.example.escort.module.order.vo.StatusLogVO;
import com.example.escort.security.RequireRole;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单模块")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class EscortOrderController {

    private final OrderService orderService;

    @Operation(summary = "费用试算")
    @PostMapping("/estimate")
    public Result<EstimateVO> estimate(@Valid @RequestBody OrderEstimateDTO dto) {
        return Result.ok(orderService.estimate(dto));
    }

    @Operation(summary = "下单（幂等+模拟支付）")
    @PostMapping
    @RequireRole("FAMILY")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.create(UserContext.getUserId(), dto));
    }

    @Operation(summary = "订单分页（按角色）")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        IPage<OrderVO> page = orderService.page(UserContext.getUserId(), UserContext.getUserRole(), current, size, status);
        return Result.ok(PageResult.of(page));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.getById(UserContext.getUserId(), UserContext.getUserRole(), id));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    @RequireRole("FAMILY")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody OrderCancelDTO dto) {
        orderService.cancel(UserContext.getUserId(), "FAMILY", id, dto);
        return Result.ok();
    }

    @Operation(summary = "管理员派单")
    @PostMapping("/{id}/dispatch")
    @RequireRole("ADMIN")
    public Result<Void> dispatch(@PathVariable Long id, @RequestParam Long escortId) {
        orderService.dispatch(UserContext.getUserId(), id, escortId);
        return Result.ok();
    }

    @Operation(summary = "陪诊师接单")
    @PostMapping("/{id}/accept")
    @RequireRole("ESCORT")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.accept(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "陪诊师拒单")
    @PostMapping("/{id}/reject")
    @RequireRole("ESCORT")
    public Result<Void> reject(@PathVariable Long id) {
        orderService.reject(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "陪诊师开始服务")
    @PostMapping("/{id}/start")
    @RequireRole("ESCORT")
    public Result<Void> start(@PathVariable Long id) {
        orderService.start(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "陪诊师结束服务")
    @PostMapping("/{id}/finish")
    @RequireRole("ESCORT")
    public Result<Void> finish(@PathVariable Long id) {
        orderService.finish(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "管理员结算")
    @PostMapping("/{id}/settle")
    @RequireRole("ADMIN")
    public Result<Void> settle(@PathVariable Long id) {
        orderService.settle(UserContext.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "家属评价")
    @PostMapping("/{id}/evaluate")
    @RequireRole("FAMILY")
    public Result<Void> evaluate(@PathVariable Long id, @Valid @RequestBody EvaluateDTO dto) {
        orderService.evaluate(UserContext.getUserId(), id, dto);
        return Result.ok();
    }

    @Operation(summary = "查询订单状态流转日志")
    @GetMapping("/{id}/status-log")
    public Result<List<StatusLogVO>> statusLog(@PathVariable Long id) {
        return Result.ok(orderService.statusLog(UserContext.getUserId(), UserContext.getUserRole(), id));
    }
}