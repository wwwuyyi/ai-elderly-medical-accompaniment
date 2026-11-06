package com.example.escort.module.escort.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 陪诊师资料修改DTO
 */
@Data
@Schema(description = "陪诊师资料修改请求")
public class EscortProfileDTO {

    @Schema(description = "服务城市")
    private String serviceCity;

    @Schema(description = "服务区域")
    private String serviceArea;

    @Schema(description = "个人简介")
    private String intro;
}