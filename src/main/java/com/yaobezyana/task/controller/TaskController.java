package com.yaobezyana.task.controller;

import com.yaobezyana.common.exception.ErrorResponse;
import com.yaobezyana.task.dto.TaskRequest;
import com.yaobezyana.task.dto.TaskResponse;
import com.yaobezyana.task.entity.TaskStatus;
import com.yaobezyana.task.service.TaskService;
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
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Управление задачами и Inbox")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Получить все задачи",
               description = "Возвращает задачи текущего пользователя с поддержкой фильтрации и сортировки")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список задач",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/tasks")
    public List<TaskResponse> getAll(
            @Parameter(description = "Фильтр по ID проекта", example = "3")
            @RequestParam(required = false) Long projectId,

            @Parameter(description = "Фильтр по статусу задачи", schema = @Schema(allowableValues = {"ACTIVE", "COMPLETED", "BLOCKED"}))
            @RequestParam(required = false) TaskStatus status,

            @Parameter(description = "Поиск по названию (без учёта регистра)", example = "отчёт")
            @RequestParam(required = false) String search,

            @Parameter(description = "Поле сортировки", schema = @Schema(allowableValues = {"title", "dueDate", "createdAt"}))
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "Направление сортировки", schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(required = false) String sortDir,

            @AuthenticationPrincipal User currentUser) {
        return taskService.getAllTasks(currentUser.getId(), projectId, status, search, sortBy, sortDir);
    }

    @Operation(summary = "Получить Inbox",
               description = "Возвращает задачи из Inbox текущего пользователя (is_inbox = true, status != COMPLETED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список задач в Inbox",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/inbox")
    public List<TaskResponse> getInbox(@AuthenticationPrincipal User currentUser) {
        return taskService.getInbox(currentUser.getId());
    }

    @Operation(summary = "Создать задачу",
               description = """
                       Создаёт новую задачу. Правила:
                       - Если `dueDate` в будущем → статус `BLOCKED`, задача не попадает в Inbox
                       - Иначе → статус `ACTIVE`
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Задача создана",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Проект не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal User currentUser) {
        return taskService.createTask(request, currentUser);
    }

    @Operation(summary = "Обновить задачу",
               description = "Обновляет поля задачи по ID. Логика статусов применяется так же, как при создании")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача обновлена",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача или проект не найдены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/api/tasks/{id}")
    public TaskResponse update(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User currentUser) {
        return taskService.updateTask(id, request, currentUser.getId());
    }

    @Operation(summary = "Удалить задачу")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Задача удалена"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(id, currentUser.getId());
    }

    @Operation(summary = "Завершить задачу",
               description = "Переводит задачу в статус `COMPLETED` и автоматически убирает из Inbox")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача завершена",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/tasks/{id}/complete")
    public TaskResponse complete(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return taskService.completeTask(id, currentUser.getId());
    }

    @Operation(summary = "Добавить задачу в Inbox",
               description = "Устанавливает `is_inbox = true`. Задача появится в выборке /api/inbox")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача добавлена в Inbox",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/tasks/{id}/inbox")
    public TaskResponse addToInbox(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return taskService.addToInbox(id, currentUser.getId());
    }

    @Operation(summary = "Убрать задачу из Inbox",
               description = "Устанавливает `is_inbox = false`. Задача остаётся в общем списке")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача убрана из Inbox",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/tasks/{id}/uninbox")
    public TaskResponse removeFromInbox(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return taskService.removeFromInbox(id, currentUser.getId());
    }
}
