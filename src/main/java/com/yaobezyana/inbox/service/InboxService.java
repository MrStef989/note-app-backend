package com.yaobezyana.inbox.service;

import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.inbox.dto.*;
import com.yaobezyana.inbox.entity.InboxNote;
import com.yaobezyana.inbox.mapper.InboxMapper;
import com.yaobezyana.inbox.repository.InboxNoteRepository;
import com.yaobezyana.project.entity.Project;
import com.yaobezyana.project.repository.ProjectRepository;
import com.yaobezyana.task.entity.Task;
import com.yaobezyana.task.repository.TaskRepository;
import com.yaobezyana.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxNoteRepository inboxNoteRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final InboxMapper inboxMapper;

    public List<InboxNoteResponse> getNotes(Long userId) {
        return inboxNoteRepository.findAllByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(inboxMapper::toResponse)
                .toList();
    }

    @Transactional
    public InboxNoteResponse createNote(CreateNoteRequest request, User currentUser) {
        InboxNote note = InboxNote.builder()
                .user(currentUser)
                .content(request.getContent())
                .build();
        return inboxMapper.toResponse(inboxNoteRepository.save(note));
    }

    @Transactional
    public void deleteNote(Long id, Long userId) {
        InboxNote note = inboxNoteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Заметка не найдена: " + id));
        inboxNoteRepository.delete(note);
    }

    @Transactional
    public ConvertNoteResponse convertNote(Long id, ConvertNoteRequest request, User currentUser) {
        InboxNote note = inboxNoteRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Заметка не найдена: " + id));

        Long createdId = switch (request.getType()) {
            case PROJECT -> {
                Project project = Project.builder()
                        .title(request.getTitle())
                        .user(currentUser)
                        .build();
                yield projectRepository.save(project).getId();
            }
            case TASK -> {
                Task.TaskBuilder builder = Task.builder()
                        .title(request.getTitle())
                        .user(currentUser);
                if (request.getProjectId() != null) {
                    Project project = projectRepository.findByIdAndUserId(request.getProjectId(), currentUser.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Проект не найден: " + request.getProjectId()));
                    builder.project(project);
                }
                yield taskRepository.save(builder.build()).getId();
            }
            case ROUTINE -> {
                Task task = Task.builder()
                        .title(request.getTitle())
                        .user(currentUser)
                        .build();
                yield taskRepository.save(task).getId();
            }
        };

        inboxNoteRepository.delete(note);

        return ConvertNoteResponse.builder()
                .type(request.getType())
                .id(createdId)
                .build();
    }
}
