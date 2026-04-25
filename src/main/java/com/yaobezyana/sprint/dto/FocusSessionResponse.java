package com.yaobezyana.sprint.dto;

import com.yaobezyana.sprint.entity.SprintStatus;
import com.yaobezyana.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Состояние сессии фокуса: текущая задача, прогресс, список доступных задач")
public class FocusSessionResponse {

    @Schema(description = "Идентификатор спринта", example = "1")
    private Long sprintId;

    @Schema(description = "Порядковый номер спринта", example = "3")
    private int sprintNumber;

    @Schema(description = "Статус спринта", example = "ACTIVE")
    private SprintStatus sprintStatus;

    @Schema(description = "Общее количество задач в спринте", example = "5")
    private int totalTasks;

    @Schema(description = "Количество завершённых задач", example = "2")
    private int completedTasks;

    @Schema(description = "Задача, которую обезьянка сейчас выполняет (null если не взята)", nullable = true)
    private TaskResponse inProgressTask;

    @Schema(description = "Проекты с незавершёнными задачами (ACTIVE/IN_PROGRESS). Группа с projectId=null — Текучка")
    private List<FocusProjectResponse> projects;
}
