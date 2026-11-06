package com.example.escort.module.auth.controller;

import com.example.escort.common.Result;
import com.example.escort.module.auth.dto.LoginDTO;
import com.example.escort.module.auth.dto.RegisterDTO;
import com.example.escort.module.auth.service.AuthService;
import com.example.escort.module.auth.vo.CaptchaVO;
import com.example.escort.module.auth.vo.LoginVO;
import com.example.escort.module.user.vo.UserInfoVO;
import com.example.escort.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * /api/auth
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ===== 1. GET /api/auth/captcha 获取图形验证码 =====
    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        CaptchaVO vo = authService.generateCaptcha();
        return Result.ok(vo);
    }

    // ===== 2. POST /api/auth/register 注册 =====
    @Operation(summary = "用户注册（FAMILY/ESCORT）")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    // ===== 3. POST /api/auth/login 登录 =====
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.ok(vo);
    }

    // ===== 4. POST /api/auth/logout 登出 =====
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(description = "Bearer Token")
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return Result.ok();
    }

    // ===== 5. GET /api/auth/info 获取当前用户信息 =====
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        Long userId = UserContext.getUserId();
        UserInfoVO vo = authService.getCurrentUserInfo(userId);
        return Result.ok(vo);
    }
}