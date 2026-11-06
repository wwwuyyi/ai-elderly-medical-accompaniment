package com.example.escort.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图形验证码响应VO
 */
@Data
@Schema(description = "验证码响应")
public class CaptchaVO {

    @Schema(description = "验证码会话标识（登录/注册时需回传）")
    private String captchaKey;

    @Schema(description = "验证码图片Base64编码", example = "data:image/png;base64,iVBOR...")
    private String captchaImage;
}