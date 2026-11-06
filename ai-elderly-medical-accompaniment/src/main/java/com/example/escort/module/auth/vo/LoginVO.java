package com.example.escort.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应VO
 */
@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色：FAMILY/ESCORT/ADMIN")
    private String role;

    @Schema(description = "头像路径")
    private String avatar;
}