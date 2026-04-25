package com.yaobezyana.calendar.mapper;

import com.yaobezyana.calendar.dto.CalendarEntryResponse;
import com.yaobezyana.calendar.entity.CalendarEntry;
import com.yaobezyana.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class CalendarMapper {

    private final TaskMapper taskMapper;

    public CalendarEntryResponse toResponse(CalendarEntry entry) {
        return CalendarEntryResponse.builder()
                .id(entry.getId())
                .note(entry.getNote())
                .date(entry.getDate())
                .createdAt(entry.getCreatedAt())
                .tasks(entry.getTasks().stream()
                        .sorted(Comparator.comparing(t -> t.getId()))
                        .map(taskMapper::toResponse)
                        .toList())
                .build();
    }
}
