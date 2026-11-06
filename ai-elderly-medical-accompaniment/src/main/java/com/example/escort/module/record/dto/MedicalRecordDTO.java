package com.example.escort.module.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "就医档案请求")
public class MedicalRecordDTO {
    @NotNull(message = "老人档案ID不能为空")
    private Long elderlyId;
    private Long orderId;
    @NotNull(message = "医院ID不能为空")
    private Long hospitalId;
    @NotBlank(message = "科室不能为空")
    private String department;
    @NotNull(message = "就诊日期不能为空")
    private LocalDate visitDate;
    private String chiefComplaint;
    private String diagnosis;
    private String prescription;
    private String remark;
}