package com.example.escort.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码DTO
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "新密码长度需为6-20位")
    @Schema(description = "新密码（6-20位）")
    private String newPassword;
}