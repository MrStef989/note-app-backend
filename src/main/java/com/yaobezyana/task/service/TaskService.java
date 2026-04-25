package com.yaobezyana.task.service;

import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.project.entity.Project;
import com.yaobezyana.project.repository.ProjectRepository;
import com.yaobezyana.sprint.entity.SprintStatus;
import com.yaobezyana.task.dto.TaskRequest;
import com.yaobezyana.task.dto.TaskResponse;
import com.yaobezyana.task.entity.Task;
import com.yaobezyana.task.entity.TaskStatus;
import com.yaobezyana.task.mapper.TaskMapper;
import com.yaobezyana.task.repository.TaskRepository;
import com.yaobezyana.task.specification.TaskSpecification;
import com.yaobezyana.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;

    public List<TaskResponse> getAllTasks(Long userId, Long projectId, TaskStatus status,
                                         String search, String sortBy, String sortDir,
                                         Boolean routine) {
        Specification<Task> spec = Specification
                .where(TaskSpecification.hasUser(userId))
                .and(TaskSpecification.hasStatus(status))
                .and(TaskSpecification.titleContains(search));

        if (Boolean.TRUE.equals(routine)) {
            spec = spec.and(TaskSpecification.hasNoProject());
        } else if (projectId != null) {
            spec = spec.and(TaskSpecification.hasProject(projectId));
        }

        Sort sort = buildSort(sortBy, sortDir);

        return taskRepository.findAll(spec, sort)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request, User currentUser) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .user(currentUser)
                .build();

        if (request.getProjectId() != null) {
            Project project = projectRepository.findByIdAndUserId(request.getProjectId(), currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + request.getProjectId()));
            task.setProject(project);
        }

        applyDueDate(task, request.getDueDate());

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, Long userId) {
        Task task = findOwned(id, userId);
        checkTaskNotInActiveSprint(task);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());

        if (request.getProjectId() != null) {
            Project project = projectRepository.findByIdAndUserId(request.getProjectId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + request.getProjectId()));
            task.setProject(project);
        } else {
            task.setProject(null);
        }

        applyDueDate(task, request.getDueDate());

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long id, Long userId) {
        Task task = findOwned(id, userId);
        checkTaskNotInActiveSprint(task);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse completeTask(Long id, Long userId) {
        Task task = findOwned(id, userId);
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Задача уже выполнена");
        }
        task.setStatus(TaskStatus.COMPLETED);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse takeTask(Long id, Long userId) {
        Task task = findOwned(id, userId);

        if (task.getStatus() != TaskStatus.ACTIVE) {
            throw new IllegalArgumentException("Можно взять только задачу со статусом ACTIVE");
        }

        if (task.getSprint() == null || task.getSprint().getStatus() != SprintStatus.ACTIVE) {
            throw new IllegalArgumentException("Задача должна быть в активном спринте");
        }

        Long sprintId = task.getSprint().getId();
        if (taskRepository.existsBySprintIdAndStatus(sprintId, TaskStatus.IN_PROGRESS)) {
            throw new IllegalArgumentException("В этом спринте уже есть задача в работе — сначала завершите её");
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    private Task findOwned(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: " + id));
    }

    private void checkTaskNotInActiveSprint(Task task) {
        if (task.getSprint() != null && task.getSprint().getStatus() == SprintStatus.ACTIVE) {
            throw new IllegalArgumentException("Нельзя изменять задачи во время активного спринта");
        }
    }

    private void applyDueDate(Task task, LocalDateTime dueDate) {
        task.setDueDate(dueDate);
        if (dueDate != null && dueDate.isAfter(LocalDateTime.now())) {
            task.setStatus(TaskStatus.BLOCKED);
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy != null ? sortBy : "") {
            case "title" -> "title";
            case "dueDate" -> "dueDate";
            default -> "createdAt";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
