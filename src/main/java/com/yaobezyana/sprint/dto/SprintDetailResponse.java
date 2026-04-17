package com.yaobezyana.sprint.dto;

import com.yaobezyana.sprint.entity.SprintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Полная информация о спринте: метаданные + проекты + задачи")
public class SprintDetailResponse {

    @Schema(description = "Идентификатор спринта", example = "1")
    private Long id;

    @Schema(description = "Название спринта", example = "Sprint 1 — MVP")
    private String title;

    @Schema(description = "Описание спринта")
    private String description;

    @Schema(description = "Цели спринта")
    private String goals;

    @Schema(description = "Статус спринта", example = "PLANNING")
    private SprintStatus status;

    @Schema(description = "Общее количество задач", example = "12")
    private int totalTasks;

    @Schema(description = "Завершённых задач", example = "4")
    private int completedTasks;

    @Schema(description = "Проекты со своими задачами")
    private List<ProjectInSprintResponse> projects;

    @Schema(description = "Дата и время старта спринта")
    private LocalDateTime startedAt;

    @Schema(description = "Дата и время завершения спринта")
    private LocalDateTime completedAt;

    @Schema(description = "Дата создания")
    private LocalDateTime createdAt;

    @Schema(description = "Дата последнего обновления")
    private LocalDateTime updatedAt;
}
