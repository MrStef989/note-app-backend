package com.yaobezyana.sprint.controller;

import com.yaobezyana.common.exception.ErrorResponse;
import com.yaobezyana.sprint.dto.*;
import com.yaobezyana.sprint.service.SprintService;
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
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Tag(name = "Sprints", description = "Управление спринтами, режим планирования и режим фокуса")
@SecurityRequirement(name = "bearerAuth")
public class SprintController {

    private final SprintService sprintService;

    // ─── Planner mode ────────────────────────────────────────────────────────

    @Operation(summary = "Получить список спринтов",
               description = "Возвращает все спринты текущего пользователя, отсортированные по дате создания (новые первые)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список спринтов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SprintSummaryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<SprintSummaryResponse> getAll(@AuthenticationPrincipal User currentUser) {
        return sprintService.getSprints(currentUser.getId());
    }

    @Operation(summary = "Получить спринт с проектами и задачами",
               description = "Возвращает полную структуру спринта: метаданные, все проекты и все задачи в них")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Детали спринта",
                    content = @Content(schema = @Schema(implementation = SprintDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public SprintDetailResponse getDetail(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return sprintService.getSprintDetail(id, currentUser.getId());
    }

    @Operation(summary = "Создать спринт",
               description = "Создаёт новый спринт в статусе PLANNING. Добавьте проекты и задачи перед запуском")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Спринт создан",
                    content = @Content(schema = @Schema(implementation = SprintSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SprintSummaryResponse create(@Valid @RequestBody SprintRequest request,
                                        @AuthenticationPrincipal User currentUser) {
        return sprintService.createSprint(request, currentUser);
    }

    @Operation(summary = "Обновить спринт",
               description = "Обновляет метаданные спринта. Доступно только в статусе PLANNING")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Спринт обновлён",
                    content = @Content(schema = @Schema(implementation = SprintSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или спринт не в статусе PLANNING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public SprintSummaryResponse update(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long id,
            @Valid @RequestBody SprintRequest request,
            @AuthenticationPrincipal User currentUser) {
        return sprintService.updateSprint(id, request, currentUser.getId());
    }

    @Operation(summary = "Удалить спринт",
               description = "Удаляет спринт. Доступно только в статусе PLANNING. Проекты спринта не удаляются, у них обнуляется sprintId")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Спринт удалён"),
            @ApiResponse(responseCode = "400", description = "Спринт не в статусе PLANNING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        sprintService.deleteSprint(id, currentUser.getId());
    }

    @Operation(summary = "Запустить спринт",
               description = """
                       Переводит спринт в статус ACTIVE — включается режим фокуса.
                       Условия:
                       - Статус должен быть PLANNING
                       - Спринт должен содержать хотя бы один проект с задачами
                       - У пользователя не должно быть другого активного спринта
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Спринт запущен",
                    content = @Content(schema = @Schema(implementation = SprintSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Условия запуска не выполнены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/start")
    public SprintSummaryResponse start(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return sprintService.startSprint(id, currentUser.getId());
    }

    @Operation(summary = "Завершить спринт",
               description = """
                       Переводит спринт в статус COMPLETED.
                       Условия:
                       - Статус должен быть ACTIVE
                       - Все задачи спринта должны иметь статус COMPLETED
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Спринт завершён",
                    content = @Content(schema = @Schema(implementation = SprintSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Не все задачи выполнены или спринт не активен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/complete")
    public SprintSummaryResponse complete(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return sprintService.completeSprint(id, currentUser.getId());
    }

    // ─── Focus mode ──────────────────────────────────────────────────────────

    @Operation(summary = "Получить сессию фокуса",
               description = """
                       Возвращает состояние фокус-сессии для обезьянки:
                       - Текущая задача в работе (`inProgressTask`, null если не взята)
                       - Прогресс (выполнено/всего)
                       - Все незавершённые задачи по проектам (только ACTIVE и IN_PROGRESS)

                       Доступен только если спринт в статусе ACTIVE.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Состояние фокус-сессии",
                    content = @Content(schema = @Schema(implementation = FocusSessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Спринт не активен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/tasks")
    public FocusSessionResponse getFocusSession(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return sprintService.getFocusSession(id, currentUser.getId());
    }

    @Operation(summary = "Переупорядочить задачи в проекте",
               description = "Задаёт порядок задач (поле position) для отображения в режиме планирования. Доступно только в статусе PLANNING")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Порядок сохранён"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или спринт не в статусе PLANNING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт или проект не найдены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{sprintId}/projects/{projectId}/tasks/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderTasks(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long sprintId,
            @Parameter(description = "ID проекта", example = "2") @PathVariable Long projectId,
            @Valid @RequestBody ReorderRequest request,
            @AuthenticationPrincipal User currentUser) {
        sprintService.reorderTasks(sprintId, projectId, request.getTaskIds(), currentUser.getId());
    }
}
