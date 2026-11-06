package com.example.escort.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号", example = "13800138000")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "图形验证码", example = "A3Bc")
    private String captcha;

    @NotBlank(message = "验证码会话标识不能为空")
    @Schema(description = "验证码会话ID（从captcha接口获取）")
    private String captchaKey;

    @NotBlank(message = "登录角色不能为空")
    @Pattern(regexp = "FAMILY|ESCORT|ADMIN", message = "登录角色无效")
    @Schema(description = "登录端角色", example = "FAMILY")
    private String role;
}