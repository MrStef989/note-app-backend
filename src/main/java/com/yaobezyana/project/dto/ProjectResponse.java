package com.yaobezyana.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Данные проекта")
public class ProjectResponse {

    @Schema(description = "Идентификатор проекта", example = "1")
    private Long id;

    @Schema(description = "Название проекта", example = "Работа")
    private String title;
}
