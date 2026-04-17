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
@Tag(name = "Tasks", description = "Управление задачами")
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

            @Parameter(description = "Фильтр по статусу задачи",
                    schema = @Schema(allowableValues = {"ACTIVE", "IN_PROGRESS", "COMPLETED", "BLOCKED"}))
            @RequestParam(required = false) TaskStatus status,

            @Parameter(description = "Поиск по названию (без учёта регистра)", example = "отчёт")
            @RequestParam(required = false) String search,

            @Parameter(description = "Поле сортировки",
                    schema = @Schema(allowableValues = {"title", "dueDate", "createdAt"}))
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "Направление сортировки",
                    schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(required = false) String sortDir,

            @AuthenticationPrincipal User currentUser) {
        return taskService.getAllTasks(currentUser.getId(), projectId, status, search, sortBy, sortDir);
    }

    @Operation(summary = "Создать задачу",
               description = """
                       Создаёт новую задачу.
                       - Если `dueDate` в будущем → статус `BLOCKED`
                       - Иначе → статус `ACTIVE`
                       - Создание задачи в проекте активного спринта запрещено (400)
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Задача создана",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или проект в активном спринте",
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
               description = "Обновляет поля задачи по ID. Запрещено если задача в проекте активного спринта")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача обновлена",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или задача в активном спринте",
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

    @Operation(summary = "Удалить задачу",
               description = "Запрещено если задача в проекте активного спринта")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Задача удалена"),
            @ApiResponse(responseCode = "400", description = "Задача в активном спринте",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
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

    @Operation(summary = "Взять задачу в работу (Focus mode)",
               description = """
                       Переводит задачу в статус `IN_PROGRESS` — обезьянка берёт задачу.
                       Условия:
                       - Задача должна быть в статусе `ACTIVE`
                       - Задача должна принадлежать проекту в активном спринте
                       - В этом спринте не должно быть другой задачи `IN_PROGRESS`
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача взята в работу",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Условия не выполнены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/tasks/{id}/take")
    public TaskResponse take(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return taskService.takeTask(id, currentUser.getId());
    }

    @Operation(summary = "Завершить задачу",
               description = "Переводит задачу в статус `COMPLETED`. В режиме фокуса применяется к задаче со статусом IN_PROGRESS")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача завершена",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Задача уже завершена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
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
}
