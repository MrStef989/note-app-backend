package com.yaobezyana.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на автодополнение текста")
public class AutocompleteRequest {

    @NotBlank
    @Schema(description = "Текст, который пользователь уже ввёл", example = "Этот проект включает разработку")
    private String currentText;
}
