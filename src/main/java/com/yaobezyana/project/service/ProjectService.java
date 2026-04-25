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
                .description(request.getDescription())
                .user(currentUser)
                .build();
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + id));
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id, Long userId) {
        Project project = projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + id));
        taskRepository.detachTasksFromProject(id);
        projectRepository.delete(project);
    }
}
