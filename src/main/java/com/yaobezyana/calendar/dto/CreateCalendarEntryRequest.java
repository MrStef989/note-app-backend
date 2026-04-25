package com.yaobezyana.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Запрос на создание записи в календаре (блокировка задач)")
public class CreateCalendarEntryRequest {

    @NotBlank
    @Schema(description = "Причина блокировки задач", example = "Ждём ответа от заказчика", requiredMode = Schema.RequiredMode.REQUIRED)
    private String note;

    @NotNull
    @Schema(description = "Дата снятия блокировки", example = "2026-05-10", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @NotEmpty
    @Schema(description = "IDs задач которые блокируются", example = "[5, 7, 12]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> taskIds;
}
