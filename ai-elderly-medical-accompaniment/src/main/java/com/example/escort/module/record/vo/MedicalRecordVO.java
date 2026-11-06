package com.example.escort.module.record.vo;

import com.example.escort.common.desensitize.SensitiveField;
import com.example.escort.common.desensitize.SensitiveType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "就医档案详情")
public class MedicalRecordVO {
    private Long id;
    private Long elderlyId;
    @SensitiveField(type = SensitiveType.NAME)
    private String elderlyName;
    @SensitiveField(type = SensitiveType.PHONE)
    private String phone;
    private Long orderId;
    private String orderNo;
    private Long hospitalId;
    private String hospitalName;
    private String department;
    private LocalDate visitDate;
    private String chiefComplaint;
    @SensitiveField(type = SensitiveType.DIAGNOSIS)
    private String diagnosis;
    private String prescription;
    private String remark;
    private LocalDateTime createTime;
    private List<MedicalDocumentVO> documents;
}