package com.yaobezyana.sprint.dto;

import com.yaobezyana.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Группа задач в спринте (проект или Текучка)")
public class ProjectInSprintResponse {

    @Schema(description = "Идентификатор проекта (null = Текучка)", example = "2", nullable = true)
    private Long id;

    @Schema(description = "Название проекта (null = Текучка)", example = "Backend", nullable = true)
    private String title;

    @Schema(description = "Задачи в данной группе")
    private List<TaskResponse> tasks;
}
