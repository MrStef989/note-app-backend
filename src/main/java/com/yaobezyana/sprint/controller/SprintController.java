package com.yaobezyana.sprint.controller;

import com.yaobezyana.ai.dto.SprintSuggestionsResponse;
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

    @Operation(summary = "История спринтов",
               description = "Возвращает все спринты пользователя (включая завершённые), отсортированные по номеру (новые первые)")
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

    @Operation(summary = "Текущий спринт",
               description = "Возвращает текущий спринт (PLANNING или ACTIVE) с задачами сгруппированными по проектам. Авто-создаёт спринт если его нет.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Текущий спринт",
                    content = @Content(schema = @Schema(implementation = SprintDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/current")
    public SprintDetailResponse getCurrent(@AuthenticationPrincipal User currentUser) {
        return sprintService.getCurrentSprintDetail(currentUser.getId());
    }

    @Operation(summary = "Спринт по ID",
               description = "Возвращает спринт по ID (для истории)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Спринт",
                    content = @Content(schema = @Schema(implementation = SprintDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Спринт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public SprintDetailResponse getById(
            @Parameter(description = "ID спринта", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return sprintService.getSprintDetail(id, currentUser.getId());
    }

    @Operation(summary = "Доступные задачи для спринта",
               description = """
                       Возвращает задачи которые можно добавить в текущий спринт, сгруппированные по проектам.
                       Исключены:
                       - Уже добавленные в спринт
                       - Заблокированные в календаре
                       - Статус BLOCKED (по dueDate), COMPLETED, IN_PROGRESS

                       Поле `sprintTaskAdded` показывает что из данного проекта уже добавлена задача (лимит исчерпан).
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список доступных задач",
                    content = @Content(schema = @Schema(implementation = AvailableTasksResponse.class))),
            @ApiResponse(responseCode = "400", description = "Нет спринта в статусе PLANNING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/current/available-tasks")
    public AvailableTasksResponse getAvailableTasks(@AuthenticationPrincipal User currentUser) {
        return sprintService.getAvailableTasks(currentUser.getId());
    }

    @Operation(summary = "Добавить задачу в текущий спринт",
               description = """
                       Добавляет задачу в текущий спринт (только в статусе PLANNING).
                       Правила:
                       - Из каждого проекта можно добавить только 1 задачу
                       - Из Текучки (задачи без проекта) тоже только 1 задачу
                       - Задача уже не должна быть в спринте
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Задача добавлена, возвращает обновлённый спринт",
                    content = @Content(schema = @Schema(implementation = SprintSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Нарушение правила 1 задача / проект",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/current/tasks/{taskId}")
    public SprintSummaryResponse addTask(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        return sprintService.addTaskToSprint(taskId, currentUser.getId());
    }

    @Operation(summary = "Убрать задачу из текущего спринта",
               description = "Убирает задачу из текущего спринта (только в статусе PLANNING)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Задача убрана из спринта"),
            @ApiResponse(responseCode = "400", description = "Спринт не в статусе PLANNING или задача не в спринте",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/current/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTask(
            @Parameter(description = "ID задачи", example = "42") @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        sprintService.removeTaskFromSprint(taskId, currentUser.getId());
    }

    @Operation(summary = "Запустить спринт",
               description = """
                       Переводит текущий спринт в статус ACTIVE — включается режим фокуса.
                       Условия:
                       - В спринте должна быть хотя бы одна задача
                       - Инбокс должен быть пустым (все заметки обработаны)
                       - Если в Текучке есть задачи — одна из них обязана быть в спринте
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Спринт запущен",
                    content = @Content(schema = @Schema(implementation = SprintSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Условия запуска не выполнены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/current/start")
    public SprintSummaryResponse start(@AuthenticationPrincipal User currentUser) {
        return sprintService.startSprint(currentUser.getId());
    }

    @Operation(summary = "Завершить спринт",
               description = """
                       Завершает активный спринт. Автоматически создаёт следующий спринт.
                       Условие: все задачи спринта должны иметь статус COMPLETED.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Спринт завершён",
                    content = @Content(schema = @Schema(implementation = SprintSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Не все задачи выполнены или нет активного спринта",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/current/complete")
    public SprintSummaryResponse complete(@AuthenticationPrincipal User currentUser) {
        return sprintService.completeSprint(currentUser.getId());
    }

    @Operation(summary = "Сессия фокуса",
               description = """
                       Возвращает состояние фокус-сессии:
                       - Текущая задача в работе (`inProgressTask`, null если не взята)
                       - Прогресс (выполнено/всего)
                       - Все незавершённые задачи по группам (проект или Текучка — группа с projectId=null)

                       Доступен только если есть ACTIVE спринт.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Состояние фокус-сессии",
                    content = @Content(schema = @Schema(implementation = FocusSessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Нет активного спринта",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/current/focus")
    public FocusSessionResponse getFocusSession(@AuthenticationPrincipal User currentUser) {
        return sprintService.getFocusSession(currentUser.getId());
    }

    @Operation(summary = "ИИ: предложение задач для спринта",
               description = """
                       Анализирует доступные задачи и предлагает по одной из каждого проекта/Текучки.
                       Только для спринта в статусе PLANNING. Предложение не изменяет данные — финальный выбор за пользователем.
                       Требует запущенного Ollama.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Предложения ИИ по задачам",
                    content = @Content(schema = @Schema(implementation = SprintSuggestionsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Нет спринта PLANNING или все группы уже заполнены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Ollama недоступна",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/current/suggest-tasks")
    public SprintSuggestionsResponse suggestTasks(@AuthenticationPrincipal User currentUser) {
        return sprintService.suggestTasks(currentUser.getId());
    }
}
