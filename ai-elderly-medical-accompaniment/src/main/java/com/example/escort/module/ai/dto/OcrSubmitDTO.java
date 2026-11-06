package com.example.escort.module.ai.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data @Schema(description = "OCR识别提交")
public class OcrSubmitDTO {
    @NotNull private Long documentId;
}