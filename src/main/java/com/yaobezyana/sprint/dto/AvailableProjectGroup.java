package com.yaobezyana.sprint.dto;

import com.yaobezyana.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Группа доступных задач для одного проекта (или Текучки)")
public class AvailableProjectGroup {

    @Schema(description = "ID проекта (null = Текучка)", example = "2", nullable = true)
    private Long projectId;

    @Schema(description = "Название проекта (null = Текучка)", example = "Backend", nullable = true)
    private String projectTitle;

    @Schema(description = "Уже добавлена задача из этого проекта в спринт (лимит исчерпан)", example = "false")
    private boolean sprintTaskAdded;

    @Schema(description = "Доступные задачи (не заблокированные, не в спринте, статус ACTIVE)")
    private List<TaskResponse> tasks;
}
