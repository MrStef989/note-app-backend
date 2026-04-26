package com.yaobezyana.ai.dto;

import com.yaobezyana.inbox.dto.ConversionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Предложение ИИ по конвертации заметки")
public class InboxSuggestionResponse {

    @Schema(description = "Предлагаемый тип конвертации", example = "TASK")
    private ConversionType type;

    @Schema(description = "Предлагаемый заголовок", example = "Починить авторизацию")
    private String suggestedTitle;

    @Schema(description = "ID проекта (только для типа TASK, иначе null)", example = "5", nullable = true)
    private Long suggestedProjectId;

    @Schema(description = "Краткое обоснование выбора", example = "Заметка описывает конкретную техническую задачу")
    private String reason;
}
