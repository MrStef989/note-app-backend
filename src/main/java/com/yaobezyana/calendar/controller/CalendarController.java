package com.yaobezyana.calendar.controller;

import com.yaobezyana.calendar.dto.CalendarEntryResponse;
import com.yaobezyana.calendar.dto.CreateCalendarEntryRequest;
import com.yaobezyana.calendar.dto.UpdateCalendarEntryRequest;
import com.yaobezyana.calendar.service.CalendarService;
import com.yaobezyana.common.exception.ErrorResponse;
import com.yaobezyana.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "Календарь блокировок задач")
@SecurityRequirement(name = "bearerAuth")
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "Список блокировок",
               description = "Возвращает все записи календаря, отсортированные по дате (ближайшие первые). Первая запись — та, что разблокируется раньше всех")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список блокировок",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CalendarEntryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<CalendarEntryResponse> getAll(@AuthenticationPrincipal User currentUser) {
        return calendarService.getAll(currentUser.getId());
    }

    @Operation(summary = "Получить блокировку по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запись найдена",
                    content = @Content(schema = @Schema(implementation = CalendarEntryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Запись не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public CalendarEntryResponse getById(
            @Parameter(description = "ID записи", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return calendarService.getById(id, currentUser.getId());
    }

    @Operation(summary = "Создать блокировку",
               description = """
                       Создаёт запись в календаре и блокирует указанные задачи.
                       Заблокированные задачи:
                       - Остаются в своём проекте
                       - Не видны при выборе задач для спринта
                       - Не могут быть добавлены в спринт
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Блокировка создана",
                    content = @Content(schema = @Schema(implementation = CalendarEntryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Одна из задач не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEntryResponse create(@Valid @RequestBody CreateCalendarEntryRequest request,
                                        @AuthenticationPrincipal User currentUser) {
        return calendarService.create(request, currentUser);
    }

    @Operation(summary = "Обновить блокировку",
               description = "Обновляет причину, дату и список заблокированных задач. Передача пустого `taskIds` снимает блокировку со всех задач")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Блокировка обновлена",
                    content = @Content(schema = @Schema(implementation = CalendarEntryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Запись или задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public CalendarEntryResponse update(
            @Parameter(description = "ID записи", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateCalendarEntryRequest request,
            @AuthenticationPrincipal User currentUser) {
        return calendarService.update(id, request, currentUser.getId());
    }

    @Operation(summary = "Удалить блокировку",
               description = "Удаляет запись из календаря. Все задачи, заблокированные этой записью, становятся доступными для спринта")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Блокировка удалена"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Запись не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID записи", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        calendarService.delete(id, currentUser.getId());
    }
}
