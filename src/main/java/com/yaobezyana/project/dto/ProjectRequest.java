package com.yaobezyana.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание или обновление проекта")
public class ProjectRequest {

    @NotBlank
    @Schema(description = "Название проекта", example = "Работа", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "ID спринта для привязки проекта (null — проект без спринта, только в статусе PLANNING)", example = "1")
    private Long sprintId;
}
