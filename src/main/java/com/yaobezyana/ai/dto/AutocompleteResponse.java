package com.yaobezyana.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Результат автодополнения текста")
public class AutocompleteResponse {

    @Schema(description = "Предлагаемое продолжение текста", example = " REST API на Spring Boot с PostgreSQL в качестве базы данных.")
    private String suggestion;
}
