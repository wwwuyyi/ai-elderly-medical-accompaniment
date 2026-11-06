package com.example.escort.module.record.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medical_record")
public class MedicalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderlyId;
    private Long orderId;
    private Long hospitalId;
    private String department;
    private LocalDate visitDate;
    private String chiefComplaint;
    private String diagnosis;
    private String prescription;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}