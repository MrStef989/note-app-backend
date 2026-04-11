package com.yaobezyana.task.service;

import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.project.entity.Project;
import com.yaobezyana.project.repository.ProjectRepository;
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

    /**
     * Возвращает все задачи пользователя с опциональной фильтрацией и сортировкой.
     *
     * @param userId    идентификатор текущего пользователя
     * @param projectId фильтр по проекту (nullable)
     * @param status    фильтр по статусу (nullable)
     * @param search    поиск по названию (nullable)
     * @param sortBy    поле сортировки: title | dueDate | createdAt (default)
     * @param sortDir   направление: asc | desc (default asc)
     */
    public List<TaskResponse> getAllTasks(Long userId, Long projectId, TaskStatus status,
                                         String search, String sortBy, String sortDir) {
        Specification<Task> spec = Specification
                .where(TaskSpecification.hasUser(userId))
                .and(TaskSpecification.hasProject(projectId))
                .and(TaskSpecification.hasStatus(status))
                .and(TaskSpecification.titleContains(search));

        Sort sort = buildSort(sortBy, sortDir);

        return taskRepository.findAll(spec, sort)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    /**
     * Возвращает задачи из Inbox: is_inbox = true и status != COMPLETED.
     */
    public List<TaskResponse> getInbox(Long userId) {
        return taskRepository.findInboxTasks(userId, TaskStatus.COMPLETED)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    /**
     * Создаёт новую задачу. Если указан due_date в будущем — статус BLOCKED, иначе ACTIVE.
     */
    @Transactional
    public TaskResponse createTask(TaskRequest request, User currentUser) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .user(currentUser)
                .build();

        if (request.getProjectId() != null) {
            Project project = projectRepository.findByIdAndUserId(request.getProjectId(), currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.getProjectId()));
            task.setProject(project);
        }

        applyDueDate(task, request.getDueDate());

        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Обновляет задачу. Пересчитывает статус при изменении due_date.
     */
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, Long userId) {
        Task task = findOwned(id, userId);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());

        if (request.getProjectId() != null) {
            Project project = projectRepository.findByIdAndUserId(request.getProjectId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.getProjectId()));
            task.setProject(project);
        } else {
            task.setProject(null);
        }

        applyDueDate(task, request.getDueDate());

        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Удаляет задачу.
     */
    public void deleteTask(Long id, Long userId) {
        Task task = findOwned(id, userId);
        taskRepository.delete(task);
    }

    /**
     * Помечает задачу как выполненную и убирает из Inbox.
     */
    public TaskResponse completeTask(Long id, Long userId) {
        Task task = findOwned(id, userId);
        task.setStatus(TaskStatus.COMPLETED);
        task.setInbox(false);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Добавляет задачу в Inbox.
     */
    public TaskResponse addToInbox(Long id, Long userId) {
        Task task = findOwned(id, userId);
        task.setInbox(true);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    /**
     * Убирает задачу из Inbox. Задача остаётся в общем списке.
     */
    public TaskResponse removeFromInbox(Long id, Long userId) {
        Task task = findOwned(id, userId);
        task.setInbox(false);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    private Task findOwned(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private void applyDueDate(Task task, LocalDateTime dueDate) {
        task.setDueDate(dueDate);
        if (dueDate != null && dueDate.isAfter(LocalDateTime.now())) {
            task.setStatus(TaskStatus.BLOCKED);
            task.setInbox(false);
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
