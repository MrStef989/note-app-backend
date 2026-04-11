package com.yaobezyana.project.service;

import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.project.dto.ProjectRequest;
import com.yaobezyana.project.dto.ProjectResponse;
import com.yaobezyana.project.entity.Project;
import com.yaobezyana.project.mapper.ProjectMapper;
import com.yaobezyana.project.repository.ProjectRepository;
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
    private final ProjectMapper projectMapper;

    /**
     * Возвращает все проекты текущего пользователя.
     */
    public List<ProjectResponse> getAllProjects(Long userId) {
        return projectRepository.findAllByUserId(userId)
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    /**
     * Создаёт новый проект для текущего пользователя.
     */
    public ProjectResponse createProject(ProjectRequest request, User currentUser) {
        Project project = Project.builder()
                .title(request.getTitle())
                .user(currentUser)
                .build();
        return projectMapper.toResponse(projectRepository.save(project));
    }

    /**
     * Обновляет заголовок проекта. Пользователь может редактировать только свои проекты.
     */
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        project.setTitle(request.getTitle());
        return projectMapper.toResponse(projectRepository.save(project));
    }

    /**
     * Удаляет проект. Связанные задачи остаются, у них обнуляется project_id.
     */
    @Transactional
    public void deleteProject(Long id, Long userId) {
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        taskRepository.detachTasksFromProject(id);
        projectRepository.delete(project);
    }
}
