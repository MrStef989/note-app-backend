package com.yaobezyana.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Ответ с описанием ошибки")
public class ErrorResponse {

    @Schema(description = "HTTP статус код", example = "404")
    private int status;

    @Schema(description = "Сообщение об ошибке", example = "Task not found")
    private String message;

    @Schema(description = "Время возникновения ошибки", example = "2026-04-11T10:00:00")
    private LocalDateTime timestamp;
}
