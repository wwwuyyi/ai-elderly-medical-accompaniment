package com.example.escort.module.ai.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class NoticeGenerateDTO { @NotNull private Long orderId; }