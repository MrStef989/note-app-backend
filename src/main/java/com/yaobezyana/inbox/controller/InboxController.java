package com.yaobezyana.inbox.controller;

import com.yaobezyana.common.exception.ErrorResponse;
import com.yaobezyana.inbox.dto.*;
import com.yaobezyana.inbox.service.InboxService;
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
@RequestMapping("/api/inbox")
@RequiredArgsConstructor
@Tag(name = "Inbox", description = "Управление заметками в инбоксе")
@SecurityRequirement(name = "bearerAuth")
public class InboxController {

    private final InboxService inboxService;

    @Operation(summary = "Получить все заметки",
               description = "Возвращает заметки инбокса, отсортированные по дате создания (старые первые)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список заметок",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = InboxNoteResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<InboxNoteResponse> getAll(@AuthenticationPrincipal User currentUser) {
        return inboxService.getNotes(currentUser.getId());
    }

    @Operation(summary = "Создать заметку",
               description = "Добавляет новую заметку в инбокс. Заметки нужно обработать до запуска спринта")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заметка создана",
                    content = @Content(schema = @Schema(implementation = InboxNoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InboxNoteResponse create(@Valid @RequestBody CreateNoteRequest request,
                                    @AuthenticationPrincipal User currentUser) {
        return inboxService.createNote(request, currentUser);
    }

    @Operation(summary = "Удалить заметку",
               description = "Удаляет заметку из инбокса без создания сущности")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Заметка удалена"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заметка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID заметки", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        inboxService.deleteNote(id, currentUser.getId());
    }

    @Operation(summary = "Конвертировать заметку",
               description = """
                       Конвертирует заметку в сущность и удаляет её из инбокса.

                       Типы конвертации:
                       - `PROJECT` — создаёт новый проект с указанным `title`
                       - `TASK` — создаёт задачу в проекте (нужен `projectId`) или в Текучке
                       - `ROUTINE` — создаёт задачу в Текучке (без проекта)
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заметка конвертирована, возвращает тип и ID созданной сущности",
                    content = @Content(schema = @Schema(implementation = ConvertNoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заметка или проект не найдены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/convert")
    public ConvertNoteResponse convert(
            @Parameter(description = "ID заметки", example = "1") @PathVariable Long id,
            @Valid @RequestBody ConvertNoteRequest request,
            @AuthenticationPrincipal User currentUser) {
        return inboxService.convertNote(id, request, currentUser);
    }
}
