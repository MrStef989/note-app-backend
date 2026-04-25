package com.yaobezyana.sprint.dto;

import com.yaobezyana.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Группа задач для режима фокуса (проект или Текучка)")
public class FocusProjectResponse {

    @Schema(description = "Идентификатор проекта (null = Текучка)", example = "2", nullable = true)
    private Long projectId;

    @Schema(description = "Название проекта (null = Текучка)", example = "Backend", nullable = true)
    private String projectTitle;

    @Schema(description = "Незавершённые задачи (ACTIVE и IN_PROGRESS)")
    private List<TaskResponse> tasks;
}
