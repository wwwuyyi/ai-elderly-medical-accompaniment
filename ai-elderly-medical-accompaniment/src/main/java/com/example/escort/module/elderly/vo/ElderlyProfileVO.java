package com.example.escort.module.elderly.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 老人档案响应VO
 */
@Data
@Schema(description = "老人档案")
public class ElderlyProfileVO {

    @Schema(description = "档案ID")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "身份证号（脱敏后）")
    private String idCard;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "住址（脱敏后）")
    private String address;

    @Schema(description = "紧急联系人电话")
    private String emergencyContact;

    @Schema(description = "紧急联系人姓名")
    private String emergencyName;

    @Schema(description = "行动能力")
    private String mobility;

    @Schema(description = "过敏史")
    private String allergyHistory;

    @Schema(description = "慢性病史")
    private String chronicDisease;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}