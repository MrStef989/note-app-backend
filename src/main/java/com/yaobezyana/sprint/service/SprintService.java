package com.yaobezyana.sprint.service;

import com.yaobezyana.ai.dto.SprintSuggestionsResponse;
import com.yaobezyana.ai.service.AiService;
import com.yaobezyana.calendar.repository.CalendarEntryRepository;
import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.inbox.repository.InboxNoteRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final InboxNoteRepository inboxNoteRepository;
    private final CalendarEntryRepository calendarEntryRepository;
    private final SprintMapper sprintMapper;
    private final TaskMapper taskMapper;
    private final AiService aiService;

    public List<SprintSummaryResponse> getSprints(Long userId) {
        return sprintRepository.findAllByUserIdOrderByNumberDesc(userId).stream()
                .map(sprint -> {
                    SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprint);
                    response.setTotalTasks((int) taskRepository.countBySprintId(sprint.getId()));
                    response.setCompletedTasks((int) taskRepository.countBySprintIdAndStatus(sprint.getId(), TaskStatus.COMPLETED));
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SprintDetailResponse getCurrentSprintDetail(Long userId) {
        Sprint sprint = getOrCreateCurrentSprint(userId);
        return buildDetailResponse(sprint);
    }

    @Transactional(readOnly = true)
    public SprintDetailResponse getSprintDetail(Long id, Long userId) {
        Sprint sprint = findOwned(id, userId);
        return buildDetailResponse(sprint);
    }

    @Transactional
    public Sprint getOrCreateCurrentSprint(Long userId) {
        return sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.PLANNING)
                .or(() -> sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.ACTIVE))
                .orElseGet(() -> createNewSprint(userId));
    }

    @Transactional
    public Sprint createNewSprintForUser(User user) {
        return createNewSprintInternal(user.getId(), user);
    }

    @Transactional
    public SprintSummaryResponse addTaskToSprint(Long taskId, Long userId) {
        Sprint sprint = sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.PLANNING)
                .orElseThrow(() -> new IllegalArgumentException("Нет активного спринта в статусе PLANNING"));

        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: " + taskId));

        if (task.getSprint() != null) {
            throw new IllegalArgumentException("Задача уже добавлена в спринт");
        }

        if (calendarEntryRepository.isTaskCalendarBlocked(task.getId())) {
            throw new IllegalArgumentException("Задача заблокирована в календаре и не может быть добавлена в спринт");
        }

        if (task.getProject() != null) {
            if (taskRepository.existsBySprintIdAndProjectId(sprint.getId(), task.getProject().getId())) {
                throw new IllegalArgumentException("Из проекта '" + task.getProject().getTitle() + "' уже добавлена задача в спринт");
            }
        } else {
            if (taskRepository.existsRoutineTaskBySprintId(sprint.getId())) {
                throw new IllegalArgumentException("Из Текучки уже добавлена задача в спринт");
            }
        }

        task.setSprint(sprint);
        taskRepository.save(task);

        SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprint);
        response.setTotalTasks((int) taskRepository.countBySprintId(sprint.getId()));
        response.setCompletedTasks((int) taskRepository.countBySprintIdAndStatus(sprint.getId(), TaskStatus.COMPLETED));
        return response;
    }

    @Transactional
    public void removeTaskFromSprint(Long taskId, Long userId) {
        Sprint sprint = sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.PLANNING)
                .orElseThrow(() -> new IllegalArgumentException("Нет активного спринта в статусе PLANNING"));

        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: " + taskId));

        if (task.getSprint() == null || !task.getSprint().getId().equals(sprint.getId())) {
            throw new IllegalArgumentException("Задача не принадлежит текущему спринту");
        }

        task.setSprint(null);
        taskRepository.save(task);
    }

    @Transactional
    public SprintSummaryResponse startSprint(Long userId) {
        Sprint sprint = sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.PLANNING)
                .orElseThrow(() -> new IllegalArgumentException("Нет спринта в статусе PLANNING"));

        long sprintTaskCount = taskRepository.countBySprintId(sprint.getId());
        if (sprintTaskCount == 0) {
            throw new IllegalArgumentException("Спринт не содержит ни одной задачи");
        }

        if (inboxNoteRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Необходимо разобрать все заметки в инбоксе перед запуском спринта");
        }

        boolean hasUnsprintedRoutine = taskRepository.countUnsprintedRoutineByUserId(userId) > 0;
        if (hasUnsprintedRoutine && !taskRepository.existsRoutineTaskBySprintId(sprint.getId())) {
            throw new IllegalArgumentException("В Текучке есть задачи — добавьте хотя бы одну в спринт");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setStartedAt(LocalDateTime.now());

        SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprintRepository.save(sprint));
        response.setTotalTasks((int) sprintTaskCount);
        response.setCompletedTasks(0);
        return response;
    }

    @Transactional
    public SprintSummaryResponse completeSprint(Long userId) {
        Sprint sprint = sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Нет активного спринта"));

        long totalTasks = taskRepository.countBySprintId(sprint.getId());
        long completedTasks = taskRepository.countBySprintIdAndStatus(sprint.getId(), TaskStatus.COMPLETED);

        if (totalTasks > 0 && completedTasks < totalTasks) {
            throw new IllegalArgumentException("Все задачи спринта должны быть выполнены перед завершением");
        }

        List<AiService.CompletedTaskInfo> taskInfos = taskRepository
                .findFocusTasks(sprint.getId(), List.of(TaskStatus.COMPLETED)).stream()
                .map(t -> new AiService.CompletedTaskInfo(
                        t.getTitle(),
                        t.getProject() != null ? t.getProject().getTitle() : null))
                .toList();
        sprint.setSummary(aiService.generateSprintSummary(taskInfos, sprint.getNumber()));

        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());
        sprintRepository.save(sprint);

        createNewSprint(userId);

        SprintSummaryResponse response = sprintMapper.toSummaryResponse(sprint);
        response.setTotalTasks((int) totalTasks);
        response.setCompletedTasks((int) completedTasks);
        return response;
    }

    @Transactional(readOnly = true)
    public SprintSuggestionsResponse suggestTasks(Long userId) {
        AvailableTasksResponse available = getAvailableTasks(userId);
        List<AvailableProjectGroup> pending = available.getGroups().stream()
                .filter(g -> !g.isSprintTaskAdded() && !g.getTasks().isEmpty())
                .toList();
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("Все группы проектов уже имеют задачу в спринте или нет доступных задач");
        }
        return aiService.suggestSprintTasks(pending);
    }

    @Transactional(readOnly = true)
    public FocusSessionResponse getFocusSession(Long userId) {
        Sprint sprint = sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Нет активного спринта. Запустите спринт сначала"));

        List<Task> focusTasks = taskRepository.findFocusTasks(
                sprint.getId(), List.of(TaskStatus.ACTIVE, TaskStatus.IN_PROGRESS));

        long totalTasks = taskRepository.countBySprintId(sprint.getId());
        long completedTasks = taskRepository.countBySprintIdAndStatus(sprint.getId(), TaskStatus.COMPLETED);

        TaskResponse inProgressTask = focusTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .findFirst()
                .map(taskMapper::toResponse)
                .orElse(null);

        Map<Long, List<Task>> tasksByProject = focusTasks.stream()
                .filter(t -> t.getProject() != null)
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        List<Task> routineTasks = focusTasks.stream()
                .filter(t -> t.getProject() == null)
                .toList();

        List<FocusProjectResponse> focusProjects = new ArrayList<>();

        tasksByProject.forEach((projectId, tasks) -> {
            Project project = tasks.get(0).getProject();
            focusProjects.add(FocusProjectResponse.builder()
                    .projectId(project.getId())
                    .projectTitle(project.getTitle())
                    .tasks(tasks.stream().map(taskMapper::toResponse).toList())
                    .build());
        });

        if (!routineTasks.isEmpty()) {
            focusProjects.add(FocusProjectResponse.builder()
                    .projectId(null)
                    .projectTitle(null)
                    .tasks(routineTasks.stream().map(taskMapper::toResponse).toList())
                    .build());
        }

        return FocusSessionResponse.builder()
                .sprintId(sprint.getId())
                .sprintNumber(sprint.getNumber())
                .sprintStatus(sprint.getStatus())
                .totalTasks((int) totalTasks)
                .completedTasks((int) completedTasks)
                .inProgressTask(inProgressTask)
                .projects(focusProjects)
                .build();
    }

    @Transactional(readOnly = true)
    public AvailableTasksResponse getAvailableTasks(Long userId) {
        Sprint sprint = sprintRepository.findFirstByUserIdAndStatus(userId, SprintStatus.PLANNING)
                .orElseThrow(() -> new IllegalArgumentException("Нет спринта в статусе PLANNING для добавления задач"));

        List<Task> eligible = taskRepository.findSprintEligibleTasks(userId);

        Map<Long, List<Task>> byProject = eligible.stream()
                .filter(t -> t.getProject() != null)
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        List<Task> routineEligible = eligible.stream()
                .filter(t -> t.getProject() == null)
                .toList();

        List<AvailableProjectGroup> groups = new ArrayList<>();

        byProject.forEach((projectId, tasks) -> {
            Project project = tasks.get(0).getProject();
            boolean sprintTaskAdded = taskRepository.existsBySprintIdAndProjectId(sprint.getId(), projectId);
            groups.add(AvailableProjectGroup.builder()
                    .projectId(project.getId())
                    .projectTitle(project.getTitle())
                    .sprintTaskAdded(sprintTaskAdded)
                    .tasks(tasks.stream().map(taskMapper::toResponse).toList())
                    .build());
        });

        boolean routineSprintTaskAdded = taskRepository.existsRoutineTaskBySprintId(sprint.getId());
        if (!routineEligible.isEmpty()) {
            groups.add(AvailableProjectGroup.builder()
                    .projectId(null)
                    .projectTitle(null)
                    .sprintTaskAdded(routineSprintTaskAdded)
                    .tasks(routineEligible.stream().map(taskMapper::toResponse).toList())
                    .build());
        }

        return AvailableTasksResponse.builder().groups(groups).build();
    }

    private SprintDetailResponse buildDetailResponse(Sprint sprint) {
        List<Task> sprintTasks = taskRepository.findFocusTasks(
                sprint.getId(),
                List.of(TaskStatus.ACTIVE, TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED, TaskStatus.COMPLETED));

        long totalTasks = taskRepository.countBySprintId(sprint.getId());
        long completedTasks = taskRepository.countBySprintIdAndStatus(sprint.getId(), TaskStatus.COMPLETED);

        Map<Long, List<Task>> tasksByProject = sprintTasks.stream()
                .filter(t -> t.getProject() != null)
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        List<Task> routineTasks = sprintTasks.stream()
                .filter(t -> t.getProject() == null)
                .toList();

        List<ProjectInSprintResponse> projectResponses = new ArrayList<>();

        tasksByProject.forEach((projectId, tasks) -> {
            Project project = tasks.get(0).getProject();
            projectResponses.add(ProjectInSprintResponse.builder()
                    .id(project.getId())
                    .title(project.getTitle())
                    .tasks(tasks.stream().map(taskMapper::toResponse).toList())
                    .build());
        });

        if (!routineTasks.isEmpty()) {
            projectResponses.add(ProjectInSprintResponse.builder()
                    .id(null)
                    .title(null)
                    .tasks(routineTasks.stream().map(taskMapper::toResponse).toList())
                    .build());
        }

        return SprintDetailResponse.builder()
                .id(sprint.getId())
                .number(sprint.getNumber())
                .status(sprint.getStatus())
                .summary(sprint.getSummary())
                .totalTasks((int) totalTasks)
                .completedTasks((int) completedTasks)
                .projects(projectResponses)
                .startedAt(sprint.getStartedAt())
                .completedAt(sprint.getCompletedAt())
                .createdAt(sprint.getCreatedAt())
                .updatedAt(sprint.getUpdatedAt())
                .build();
    }

    private Sprint createNewSprint(Long userId) {
        int nextNumber = sprintRepository.findMaxNumberByUserId(userId) + 1;
        Sprint sprint = Sprint.builder()
                .user(User.builder().id(userId).build())
                .number(nextNumber)
                .build();
        return sprintRepository.save(sprint);
    }

    private Sprint createNewSprintInternal(Long userId, User user) {
        int nextNumber = sprintRepository.findMaxNumberByUserId(userId) + 1;
        Sprint sprint = Sprint.builder()
                .user(user)
                .number(nextNumber)
                .build();
        return sprintRepository.save(sprint);
    }

    private Sprint findOwned(Long id, Long userId) {
        return sprintRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Спринт не найден: " + id));
    }
}
