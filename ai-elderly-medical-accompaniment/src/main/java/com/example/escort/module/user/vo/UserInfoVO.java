package com.example.escort.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应VO
 */
@Data
@Schema(description = "用户信息")
public class UserInfoVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名（手机号）")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像路径")
    private String avatar;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "账号状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}