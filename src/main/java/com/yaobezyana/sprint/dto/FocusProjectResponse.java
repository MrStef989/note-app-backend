package com.yaobezyana.sprint.dto;

import com.yaobezyana.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Проект с незавершёнными задачами для режима фокуса")
public class FocusProjectResponse {

    @Schema(description = "Идентификатор проекта", example = "2")
    private Long projectId;

    @Schema(description = "Название проекта", example = "Backend")
    private String projectTitle;

    @Schema(description = "Незавершённые задачи проекта (ACTIVE и IN_PROGRESS)")
    private List<TaskResponse> tasks;
}
