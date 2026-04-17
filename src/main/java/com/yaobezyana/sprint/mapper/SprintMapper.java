package com.yaobezyana.sprint.mapper;

import com.yaobezyana.sprint.dto.SprintSummaryResponse;
import com.yaobezyana.sprint.entity.Sprint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SprintMapper {

    @Mapping(target = "totalTasks", ignore = true)
    @Mapping(target = "completedTasks", ignore = true)
    SprintSummaryResponse toSummaryResponse(Sprint sprint);
}
