package com.yaobezyana.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Запрос на создание или обновление задачи")
public class TaskRequest {

    @NotBlank
    @Schema(description = "Название задачи", example = "Написать отчёт", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Описание задачи", example = "Подготовить квартальный отчёт по продажам")
    private String description;

    @Schema(description = "ID проекта (null — задача без проекта)", example = "3")
    private Long projectId;

    @Schema(description = "Срок выполнения. Если дата в будущем — задача создаётся со статусом BLOCKED", example = "2026-04-15T09:00:00")
    private LocalDateTime dueDate;
}
