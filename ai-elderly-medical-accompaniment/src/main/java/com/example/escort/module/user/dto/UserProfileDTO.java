package com.example.escort.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户资料修改DTO
 */
@Data
@Schema(description = "用户资料修改请求")
public class UserProfileDTO {

    @Schema(description = "昵称", example = "张先生")
    private String nickname;

    @Schema(description = "联系电话", example = "13800138000")
    private String phone;
}