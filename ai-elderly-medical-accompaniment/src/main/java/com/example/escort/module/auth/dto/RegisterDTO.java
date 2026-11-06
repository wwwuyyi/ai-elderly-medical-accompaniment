package com.example.escort.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度需为6-20位")
    @Schema(description = "密码（6-20位）", example = "123456")
    private String password;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "FAMILY|ESCORT", message = "角色值无效，仅支持FAMILY或ESCORT")
    @Schema(description = "角色：FAMILY-家属 / ESCORT-陪诊师", example = "FAMILY")
    private String role;

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "用户昵称", example = "张先生")
    private String nickname;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "图形验证码", example = "A3Bc")
    private String captcha;

    @NotBlank(message = "验证码会话标识不能为空")
    @Schema(description = "验证码会话ID（从captcha接口获取）")
    private String captchaKey;
}