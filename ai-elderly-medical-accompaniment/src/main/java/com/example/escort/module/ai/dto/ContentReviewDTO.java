package com.example.escort.module.ai.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ContentReviewDTO {
    @NotBlank private String reviewStatus;
    private String reviewRemark;
}