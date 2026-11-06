package com.example.escort.module.elderly.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 老人档案新增/修改DTO
 */
@Data
@Schema(description = "老人档案请求")
public class ElderlyProfileDTO {

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "老人姓名", example = "张老伯")
    private String name;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "MALE|FEMALE", message = "性别值无效")
    @Schema(description = "性别：MALE-男 / FEMALE-女", example = "MALE")
    private String gender;

    @NotNull(message = "出生日期不能为空")
    @Schema(description = "出生日期", example = "1950-05-20")
    private LocalDate birthDate;

    @NotBlank(message = "身份证号不能为空")
    @Schema(description = "身份证号（系统加密存储）", example = "330102195005201234")
    private String idCard;

    @Schema(description = "联系电话", example = "13800138001")
    private String phone;

    @Schema(description = "住址", example = "广州市越秀区某小区1栋")
    private String address;

    @Schema(description = "紧急联系人电话", example = "13900139000")
    private String emergencyContact;

    @Schema(description = "紧急联系人姓名", example = "张小明")
    private String emergencyName;

    @Schema(description = "行动能力：WALK-自主行走 / ASSIST-需搀扶 / WHEELCHAIR-轮椅", example = "WALK")
    private String mobility;

    @Schema(description = "过敏史")
    private String allergyHistory;

    @Schema(description = "慢性病史")
    private String chronicDisease;

    @Schema(description = "备注")
    private String remark;
}