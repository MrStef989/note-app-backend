package com.yaobezyana.calendar.dto;

import com.yaobezyana.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Запись в календаре — блокиратор задач")
public class CalendarEntryResponse {

    @Schema(description = "Идентификатор записи", example = "1")
    private Long id;

    @Schema(description = "Причина блокировки", example = "Ждём ответа от заказчика")
    private String note;

    @Schema(description = "Дата снятия блокировки (ожидаемая)", example = "2026-05-10")
    private LocalDate date;

    @Schema(description = "Заблокированные задачи")
    private List<TaskResponse> tasks;

    @Schema(description = "Дата создания")
    private LocalDateTime createdAt;
}
