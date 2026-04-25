package com.yaobezyana.sprint.dto;

import com.yaobezyana.sprint.entity.SprintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Полная информация о спринте: метаданные + задачи сгруппированные по проектам")
public class SprintDetailResponse {

    @Schema(description = "Идентификатор спринта", example = "1")
    private Long id;

    @Schema(description = "Порядковый номер спринта", example = "3")
    private int number;

    @Schema(description = "Статус спринта", example = "PLANNING")
    private SprintStatus status;

    @Schema(description = "Общее количество задач", example = "5")
    private int totalTasks;

    @Schema(description = "Завершённых задач", example = "2")
    private int completedTasks;

    @Schema(description = "Задачи сгруппированные по проектам (null-проект = Текучка)")
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
