package com.yaobezyana.project.mapper;

import com.yaobezyana.project.dto.ProjectRequest;
import com.yaobezyana.project.dto.ProjectResponse;
import com.yaobezyana.project.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(source = "sprint.id", target = "sprintId")
    ProjectResponse toResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    Project toEntity(ProjectRequest request);
}
