package com.yaobezyana.project.controller;

import com.yaobezyana.common.exception.ErrorResponse;
import com.yaobezyana.project.dto.ProjectRequest;
import com.yaobezyana.project.dto.ProjectResponse;
import com.yaobezyana.project.service.ProjectService;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Управление проектами")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Получить все проекты",
               description = "Возвращает список всех проектов текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список проектов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public List<ProjectResponse> getAll(@AuthenticationPrincipal User currentUser) {
        return projectService.getAllProjects(currentUser.getId());
    }

    @Operation(summary = "Создать проект",
               description = "Создаёт новый проект. Проекты существуют независимо от спринтов")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Проект создан",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request,
                                  @AuthenticationPrincipal User currentUser) {
        return projectService.createProject(request, currentUser);
    }

    @Operation(summary = "Обновить проект",
               description = "Обновляет название и описание проекта по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Проект обновлён",
                    content = @Content(schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Проект не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ProjectResponse update(
            @Parameter(description = "ID проекта", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        return projectService.updateProject(id, request, currentUser.getId());
    }

    @Operation(summary = "Удалить проект",
               description = "Удаляет проект. Задачи переходят в Текучку (projectId = null)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Проект удалён"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Проект не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID проекта", example = "1") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        projectService.deleteProject(id, currentUser.getId());
    }
}
