package com.example.escort.module.escort.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 陪诊师资质申请DTO
 */
@Data
@Schema(description = "陪诊师资质申请请求")
public class EscortApplyDTO {

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "李明")
    private String realName;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "MALE|FEMALE", message = "性别值无效")
    @Schema(description = "性别：MALE-男 / FEMALE-女", example = "MALE")
    private String gender;

    @NotNull(message = "出生日期不能为空")
    @Schema(description = "出生日期", example = "1990-03-15")
    private LocalDate birthDate;

    @NotBlank(message = "身份证号不能为空")
    @Schema(description = "身份证号（系统加密存储）", example = "330102199003151234")
    private String idCard;

    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "联系电话", example = "13700137000")
    private String phone;

    @NotBlank(message = "服务城市不能为空")
    @Schema(description = "服务城市", example = "广州")
    private String serviceCity;

    @Schema(description = "服务区域", example = "越秀区,天河区")
    private String serviceArea;

    @Schema(description = "个人简介", example = "5年陪诊经验，熟悉各三甲医院就诊流程")
    private String intro;

    @Schema(description = "资质材料图片路径JSON数组", example = "[\"/upload/cert1.jpg\",\"/upload/cert2.jpg\"]")
    private String materials;
}