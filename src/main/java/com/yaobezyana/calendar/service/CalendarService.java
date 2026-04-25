package com.yaobezyana.calendar.service;

import com.yaobezyana.calendar.dto.CalendarEntryResponse;
import com.yaobezyana.calendar.dto.CreateCalendarEntryRequest;
import com.yaobezyana.calendar.dto.UpdateCalendarEntryRequest;
import com.yaobezyana.calendar.entity.CalendarEntry;
import com.yaobezyana.calendar.mapper.CalendarMapper;
import com.yaobezyana.calendar.repository.CalendarEntryRepository;
import com.yaobezyana.common.exception.ResourceNotFoundException;
import com.yaobezyana.task.entity.Task;
import com.yaobezyana.task.repository.TaskRepository;
import com.yaobezyana.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEntryRepository calendarEntryRepository;
    private final TaskRepository taskRepository;
    private final CalendarMapper calendarMapper;

    public List<CalendarEntryResponse> getAll(Long userId) {
        return calendarEntryRepository.findAllByUserIdOrderByDateAsc(userId)
                .stream()
                .map(calendarMapper::toResponse)
                .toList();
    }

    public CalendarEntryResponse getById(Long id, Long userId) {
        CalendarEntry entry = findOwned(id, userId);
        return calendarMapper.toResponse(entry);
    }

    @Transactional
    public CalendarEntryResponse create(CreateCalendarEntryRequest request, User currentUser) {
        Set<Task> tasks = resolveTasks(request.getTaskIds(), currentUser.getId());

        CalendarEntry entry = CalendarEntry.builder()
                .user(currentUser)
                .note(request.getNote())
                .date(request.getDate())
                .tasks(tasks)
                .build();

        return calendarMapper.toResponse(calendarEntryRepository.save(entry));
    }

    @Transactional
    public CalendarEntryResponse update(Long id, UpdateCalendarEntryRequest request, Long userId) {
        CalendarEntry entry = findOwned(id, userId);

        entry.setNote(request.getNote());
        entry.setDate(request.getDate());

        Set<Task> tasks = request.getTaskIds() != null
                ? resolveTasks(request.getTaskIds(), userId)
                : new HashSet<>();
        entry.setTasks(tasks);

        return calendarMapper.toResponse(calendarEntryRepository.save(entry));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        CalendarEntry entry = findOwned(id, userId);
        calendarEntryRepository.delete(entry);
    }

    private Set<Task> resolveTasks(List<Long> taskIds, Long userId) {
        Set<Task> tasks = new HashSet<>();
        for (Long taskId : taskIds) {
            Task task = taskRepository.findByIdAndUserId(taskId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена: " + taskId));
            tasks.add(task);
        }
        return tasks;
    }

    private CalendarEntry findOwned(Long id, Long userId) {
        return calendarEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись в календаре не найдена: " + id));
    }
}
