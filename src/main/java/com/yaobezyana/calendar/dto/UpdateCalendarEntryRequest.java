package com.yaobezyana.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Запрос на обновление записи в календаре")
public class UpdateCalendarEntryRequest {

    @NotBlank
    @Schema(description = "Причина блокировки", example = "Ждём ответа от заказчика", requiredMode = Schema.RequiredMode.REQUIRED)
    private String note;

    @NotNull
    @Schema(description = "Дата снятия блокировки", example = "2026-05-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @Schema(description = "IDs задач которые блокируются (пустой список = снять блокировку со всех задач)", example = "[5, 7]")
    private List<Long> taskIds;
}
