package com.example.escort.module.user.controller;

import com.example.escort.common.Result;
import com.example.escort.module.user.dto.ChangePasswordDTO;
import com.example.escort.module.user.dto.UserProfileDTO;
import com.example.escort.module.user.service.UserService;
import com.example.escort.module.user.vo.UserInfoVO;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * /api/user
 */
@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ===== 6. GET /api/user/profile 查询个人资料 =====
    @Operation(summary = "查询个人资料")
    @GetMapping("/profile")
    public Result<UserInfoVO> getProfile() {
        UserInfoVO vo = userService.getProfile(UserContext.getUserId());
        return Result.ok(vo);
    }

    // ===== 7. PUT /api/user/profile 修改个人资料 =====
    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileDTO dto) {
        userService.updateProfile(UserContext.getUserId(), dto);
        return Result.ok();
    }

    // ===== 8. PUT /api/user/password 修改密码 =====
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.getUserId(), dto);
        return Result.ok();
    }

    // ===== 9. POST /api/user/avatar 上传头像 =====
    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam String avatarPath) {
        String path = userService.updateAvatar(UserContext.getUserId(), avatarPath);
        return Result.ok(path);
    }
}