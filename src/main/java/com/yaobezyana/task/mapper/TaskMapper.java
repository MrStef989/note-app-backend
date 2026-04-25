package com.yaobezyana.task.mapper;

import com.yaobezyana.task.dto.TaskResponse;
import com.yaobezyana.task.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.title", target = "projectTitle")
    @Mapping(source = "sprint.id", target = "sprintId")
    @Mapping(source = "position", target = "position")
    TaskResponse toResponse(Task task);
}
