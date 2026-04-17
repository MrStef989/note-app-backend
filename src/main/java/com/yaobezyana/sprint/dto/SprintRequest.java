package com.yaobezyana.sprint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание или обновление спринта")
public class SprintRequest {

    @NotBlank
    @Schema(description = "Название спринта", example = "Sprint 1 — MVP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Описание спринта", example = "Основные фичи для первого релиза")
    private String description;

    @Schema(description = "Цели спринта", example = "Реализовать авторизацию, CRUD задач и режим фокуса")
    private String goals;
}
