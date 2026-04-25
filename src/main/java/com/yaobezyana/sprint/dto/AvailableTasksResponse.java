package com.yaobezyana.sprint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Задачи доступные для добавления в текущий спринт, сгруппированные по проектам")
public class AvailableTasksResponse {

    @Schema(description = "Группы задач по проектам (группа с projectId=null — Текучка)")
    private List<AvailableProjectGroup> groups;
}
