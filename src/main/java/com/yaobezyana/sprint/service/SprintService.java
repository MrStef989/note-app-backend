package com.yaobezyana.sprint.service;

import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.project.entity.Project;
import com.yaobezyana.project.repository.ProjectRepository;
import com.yaobezyana.sprint.dto.*;
import com.yaobezyana.sprint.entity.Sprint;
import com.yaobezyana.sprint.entity.SprintStatus;
import com.yaobezyana.sprint.mapper.SprintMapper;
import com.yaobezyana.sprint.repository.SprintRepository;
import com.yaobezyana.task.dto.TaskResponse;
import com.yaobezyana.task.entity.Task;
import com.yaobezyana.task.entity.TaskStatus;
import com.yaobezyana.task.mapper.TaskMapper;
import com.yaobezyana.task.repository.TaskRepository;
import com.yaobezyana.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final SprintMapper sprintMapper;
    private final TaskMapper taskMapper;

    public List<SprintSummaryResponse> getSprints(Long userId) {
        return sprintRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(sprint -> {
                    SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprint);
                    response.setTotalTasks((int) taskRepository.countBySprintId(sprint.getId()));
                    response.setCompletedTasks((int) taskRepository.countBySprintIdAndStatus(sprint.getId(), TaskStatus.COMPLETED));
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SprintDetailResponse getSprintDetail(Long id, Long userId) {
        Sprint sprint = findOwned(id, userId);
        List<Project> projects = projectRepository.findAllBySprintId(id);

        long totalTasks = taskRepository.countBySprintId(id);
        long completedTasks = taskRepository.countBySprintIdAndStatus(id, TaskStatus.COMPLETED);

        List<ProjectInSprintResponse> projectResponses = projects.stream()
                .map(p -> {
                    List<TaskResponse> tasks = taskRepository.findByProjectId(p.getId()).stream()
                            .map(taskMapper::toResponse)
                            .toList();
                    return ProjectInSprintResponse.builder()
                            .id(p.getId())
                            .title(p.getTitle())
                            .tasks(tasks)
                            .build();
                })
                .toList();

        return SprintDetailResponse.builder()
                .id(sprint.getId())
                .title(sprint.getTitle())
                .description(sprint.getDescription())
                .goals(sprint.getGoals())
                .status(sprint.getStatus())
                .totalTasks((int) totalTasks)
                .completedTasks((int) completedTasks)
                .projects(projectResponses)
                .startedAt(sprint.getStartedAt())
                .completedAt(sprint.getCompletedAt())
                .createdAt(sprint.getCreatedAt())
                .updatedAt(sprint.getUpdatedAt())
                .build();
    }

    @Transactional
    public SprintSummaryResponse createSprint(SprintRequest request, User currentUser) {
        Sprint sprint = Sprint.builder()
                .user(currentUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .goals(request.getGoals())
                .build();
        Sprint saved = sprintRepository.save(sprint);
        SprintSummaryResponse response = sprintMapper.toSummaryResponse(saved);
        response.setTotalTasks(0);
        response.setCompletedTasks(0);
        return response;
    }

    @Transactional
    public SprintSummaryResponse updateSprint(Long id, SprintRequest request, Long userId) {
        Sprint sprint = findOwned(id, userId);
        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new IllegalArgumentException("Спринт можно редактировать только в статусе PLANNING");
        }
        sprint.setTitle(request.getTitle());
        sprint.setDescription(request.getDescription());
        sprint.setGoals(request.getGoals());
        SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprintRepository.save(sprint));
        response.setTotalTasks((int) taskRepository.countBySprintId(id));
        response.setCompletedTasks((int) taskRepository.countBySprintIdAndStatus(id, TaskStatus.COMPLETED));
        return response;
    }

    @Transactional
    public void deleteSprint(Long id, Long userId) {
        Sprint sprint = findOwned(id, userId);
        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new IllegalArgumentException("Удалить можно только спринт в статусе PLANNING");
        }
        // Открепляем проекты от спринта перед удалением
        List<Project> projects = projectRepository.findAllBySprintId(id);
        projects.forEach(p -> p.setSprint(null));
        projectRepository.saveAll(projects);
        sprintRepository.delete(sprint);
    }

    @Transactional
    public SprintSummaryResponse startSprint(Long id, Long userId) {
        Sprint sprint = findOwned(id, userId);
        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new IllegalArgumentException("Запустить можно только спринт в статусе PLANNING");
        }
        if (sprintRepository.existsByUserIdAndStatus(userId, SprintStatus.ACTIVE)) {
            throw new IllegalArgumentException("У вас уже есть активный спринт");
        }
        List<Project> projects = projectRepository.findAllBySprintId(id);
        boolean hasTask = projects.stream()
                .anyMatch(p -> taskRepository.countIncompleteByProjectId(p.getId(), TaskStatus.COMPLETED) > 0);
        if (!hasTask) {
            throw new IllegalArgumentException("Спринт должен содержать хотя бы один проект с задачами");
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setStartedAt(LocalDateTime.now());
        SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprintRepository.save(sprint));
        response.setTotalTasks((int) taskRepository.countBySprintId(id));
        response.setCompletedTasks((int) taskRepository.countBySprintIdAndStatus(id, TaskStatus.COMPLETED));
        return response;
    }

    @Transactional
    public SprintSummaryResponse completeSprint(Long id, Long userId) {
        Sprint sprint = findOwned(id, userId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IllegalArgumentException("Завершить можно только активный спринт");
        }
        long totalTasks = taskRepository.countBySprintId(id);
        long completedTasks = taskRepository.countBySprintIdAndStatus(id, TaskStatus.COMPLETED);
        if (totalTasks > 0 && completedTasks < totalTasks) {
            throw new IllegalArgumentException("Все задачи спринта должны быть выполнены перед завершением");
        }
        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());
        SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprintRepository.save(sprint));
        response.setTotalTasks((int) totalTasks);
        response.setCompletedTasks((int) completedTasks);
        return response;
    }

    @Transactional(readOnly = true)
    public FocusSessionResponse getFocusSession(Long id, Long userId) {
        Sprint sprint = findOwned(id, userId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IllegalArgumentException("Режим фокуса доступен только для активного спринта");
        }

        List<Task> focusTasks = taskRepository.findFocusTasks(
                id, List.of(TaskStatus.ACTIVE, TaskStatus.IN_PROGRESS));

        long totalTasks = taskRepository.countBySprintId(id);
        long completedTasks = taskRepository.countBySprintIdAndStatus(id, TaskStatus.COMPLETED);

        TaskResponse inProgressTask = focusTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .findFirst()
                .map(taskMapper::toResponse)
                .orElse(null);

        List<Project> projects = projectRepository.findAllBySprintId(id);
        Map<Long, List<Task>> tasksByProject = focusTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        List<FocusProjectResponse> focusProjects = projects.stream()
                .filter(p -> tasksByProject.containsKey(p.getId()))
                .map(p -> FocusProjectResponse.builder()
                        .projectId(p.getId())
                        .projectTitle(p.getTitle())
                        .tasks(tasksByProject.get(p.getId()).stream()
                                .map(taskMapper::toResponse)
                                .toList())
                        .build())
                .toList();

        return FocusSessionResponse.builder()
                .sprintId(sprint.getId())
                .sprintTitle(sprint.getTitle())
                .sprintStatus(sprint.getStatus())
                .totalTasks((int) totalTasks)
                .completedTasks((int) completedTasks)
                .inProgressTask(inProgressTask)
                .projects(focusProjects)
                .build();
    }

    @Transactional
    public void reorderTasks(Long sprintId, Long projectId, List<Long> taskIds, Long userId) {
        Sprint sprint = findOwned(sprintId, userId);
        if (sprint.getStatus() == SprintStatus.ACTIVE) {
            throw new IllegalArgumentException("Нельзя менять порядок задач в активном спринте");
        }
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + projectId));
        if (project.getSprint() == null || !project.getSprint().getId().equals(sprintId)) {
            throw new IllegalArgumentException("Проект не принадлежит данному спринту");
        }
        List<Task> tasks = taskRepository.findAllById(taskIds);
        Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        for (int i = 0; i < taskIds.size(); i++) {
            Task task = taskMap.get(taskIds.get(i));
            if (task != null && task.getProject().getId().equals(projectId)) {
                task.setPosition(i);
            }
        }
        taskRepository.saveAll(tasks);
    }

    private Sprint findOwned(Long id, Long userId) {
        return sprintRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Спринт не найден: " + id));
    }
}
