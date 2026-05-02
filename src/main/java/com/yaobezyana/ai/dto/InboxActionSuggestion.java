package com.yaobezyana.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Одно предложенное ИИ действие для заметки")
public class InboxActionSuggestion {

    @Schema(description = "Тип действия", example = "CREATE_TASK")
    private InboxAction action;

    @Schema(description = "Предлагаемый заголовок задачи/проекта/записи", example = "Починить авторизацию")
    private String title;

    @Schema(description = "ID проекта (только для CREATE_TASK, иначе null)", example = "5", nullable = true)
    private Long projectId;

    @Schema(description = "Название проекта (только для CREATE_TASK, иначе null)", example = "Backend", nullable = true)
    private String projectTitle;

    @Schema(description = "Краткое обоснование", example = "Заметка описывает конкретную техническую задачу для существующего проекта")
    private String reason;
}
