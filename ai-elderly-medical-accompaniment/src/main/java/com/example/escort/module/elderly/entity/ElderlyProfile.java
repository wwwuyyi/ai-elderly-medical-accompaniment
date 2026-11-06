package com.example.escort.module.elderly.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 老人档案实体
 */
@Data
@TableName("elderly_profile")
public class ElderlyProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属家属用户ID */
    private Long familyId;

    /** 姓名 */
    private String name;

    /** 性别：MALE / FEMALE */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 身份证号（AES密文存储） */
    private String idCard;

    /** 联系电话 */
    private String phone;

    /** 住址（AES密文存储） */
    private String address;

    /** 紧急联系人电话 */
    private String emergencyContact;

    /** 紧急联系人姓名 */
    private String emergencyName;

    /** 行动能力：WALK / ASSIST / WHEELCHAIR */
    private String mobility;

    /** 过敏史 */
    private String allergyHistory;

    /** 慢性病史 */
    private String chronicDisease;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}