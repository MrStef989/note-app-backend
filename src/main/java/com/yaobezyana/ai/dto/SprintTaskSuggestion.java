package com.yaobezyana.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Предложение ИИ по задаче для включения в спринт")
public class SprintTaskSuggestion {

    @Schema(description = "ID проекта (null = Текучка)", example = "1", nullable = true)
    private Long projectId;

    @Schema(description = "Название проекта (null = Текучка)", example = "Backend", nullable = true)
    private String projectTitle;

    @Schema(description = "ID рекомендуемой задачи", example = "7")
    private Long taskId;

    @Schema(description = "Название рекомендуемой задачи", example = "Рефакторинг сервиса задач")
    private String taskTitle;

    @Schema(description = "Обоснование выбора", example = "Задача блокирует реализацию следующей фичи")
    private String reason;
}
