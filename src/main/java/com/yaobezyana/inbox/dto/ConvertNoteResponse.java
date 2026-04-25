package com.yaobezyana.inbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Результат конвертации заметки")
public class ConvertNoteResponse {

    @Schema(description = "Тип созданной сущности", example = "TASK")
    private ConversionType type;

    @Schema(description = "ID созданной сущности", example = "15")
    private Long id;
}
