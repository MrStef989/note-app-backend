package com.yaobezyana.inbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание заметки в инбоксе")
public class CreateNoteRequest {

    @NotBlank
    @Schema(description = "Содержимое заметки", example = "Разобраться с CI/CD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
