package com.yaobezyana.inbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на конвертацию заметки")
public class ConvertNoteRequest {

    @NotNull
    @Schema(description = "Тип конвертации", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PROJECT", "TASK", "ROUTINE"})
    private ConversionType type;

    @NotBlank
    @Schema(description = "Название создаваемой сущности", example = "Написать тесты", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "ID проекта (только для type=TASK)", example = "3")
    private Long projectId;
}
