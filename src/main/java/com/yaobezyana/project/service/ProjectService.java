package com.yaobezyana.project.service;

import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.project.dto.ProjectRequest;
import com.yaobezyana.project.dto.ProjectResponse;
import com.yaobezyana.project.entity.Project;
import com.yaobezyana.project.mapper.ProjectMapper;
import com.yaobezyana.project.repository.ProjectRepository;
import com.yaobezyana.sprint.entity.Sprint;
import com.yaobezyana.sprint.entity.SprintStatus;
import com.yaobezyana.sprint.repository.SprintRepository;
import com.yaobezyana.task.repository.TaskRepository;
import com.yaobezyana.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final ProjectMapper projectMapper;

    public List<ProjectResponse> getAllProjects(Long userId) {
        return projectRepository.findAllByUserId(userId)
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, User currentUser) {
        Project project = Project.builder()
                .title(request.getTitle())
                .user(currentUser)
                .build();

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findByIdAndUserId(request.getSprintId(), currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Спринт не найден: " + request.getSprintId()));
            if (sprint.getStatus() != SprintStatus.PLANNING) {
                throw new IllegalArgumentException("Добавлять проекты можно только в спринт со статусом PLANNING");
            }
            project.setSprint(sprint);
        }

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + id));

        checkSprintNotActive(project);

        project.setTitle(request.getTitle());

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findByIdAndUserId(request.getSprintId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Спринт не найден: " + request.getSprintId()));
            if (sprint.getStatus() != SprintStatus.PLANNING) {
                throw new IllegalArgumentException("Привязать проект можно только к спринту в статусе PLANNING");
            }
            project.setSprint(sprint);
        } else {
            project.setSprint(null);
        }

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id, Long userId) {
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + id));
        checkSprintNotActive(project);
        taskRepository.detachTasksFromProject(id);
        projectRepository.delete(project);
    }

    private void checkSprintNotActive(Project project) {
        if (project.getSprint() != null && project.getSprint().getStatus() == SprintStatus.ACTIVE) {
            throw new IllegalArgumentException("Нельзя изменять проект во время активного спринта");
        }
    }
}
