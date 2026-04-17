package com.yaobezyana.sprint.dto;

import com.yaobezyana.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Проект внутри спринта с задачами")
public class ProjectInSprintResponse {

    @Schema(description = "Идентификатор проекта", example = "2")
    private Long id;

    @Schema(description = "Название проекта", example = "Backend")
    private String title;

    @Schema(description = "Задачи проекта")
    private List<TaskResponse> tasks;
}
