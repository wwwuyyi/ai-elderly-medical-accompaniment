package com.example.escort.module.notify.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.escort.common.PageResult;
import com.example.escort.common.Result;
import com.example.escort.module.notify.entity.SysNotification;
import com.example.escort.module.notify.service.NotifyService;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知模块")
@RestController @RequestMapping("/api/notification") @RequiredArgsConstructor
public class NotifyController {
    private final NotifyService notifyService;

    @Operation(summary = "通知分页")
    @GetMapping("/page")
    public Result<PageResult<SysNotification>> page(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        IPage<SysNotification> p = notifyService.page(UserContext.getUserId(), current, size);
        return Result.ok(PageResult.of(p));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notifyService.markRead(UserContext.getUserId(), id); return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notifyService.markAllRead(UserContext.getUserId()); return Result.ok();
    }
}