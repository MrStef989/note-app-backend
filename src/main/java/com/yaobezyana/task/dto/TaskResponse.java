package com.yaobezyana.task.dto;

import com.yaobezyana.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Данные задачи")
public class TaskResponse {

    @Schema(description = "Идентификатор задачи", example = "42")
    private Long id;

    @Schema(description = "Название задачи", example = "Написать отчёт")
    private String title;

    @Schema(description = "Описание задачи", example = "Подготовить квартальный отчёт по продажам")
    private String description;

    @Schema(description = "ID проекта (null — задача без проекта)", example = "3")
    private Long projectId;

    @Schema(description = "Название проекта", example = "Работа")
    private String projectTitle;

    @Schema(description = "ID спринта, к которому относится проект задачи (null если не в спринте)", example = "1")
    private Long sprintId;

    @Schema(description = "Статус задачи", example = "ACTIVE")
    private TaskStatus status;

    @Schema(description = "Позиция задачи внутри проекта для сортировки (0-based)", example = "0")
    private int position;

    @Schema(description = "Срок выполнения", example = "2026-04-15T09:00:00")
    private LocalDateTime dueDate;

    @Schema(description = "Дата создания", example = "2026-04-11T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата последнего обновления", example = "2026-04-11T10:30:00")
    private LocalDateTime updatedAt;
}
