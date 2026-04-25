package com.yaobezyana.task.scheduler;

import com.yaobezyana.task.entity.Task;
import com.yaobezyana.task.entity.TaskStatus;
import com.yaobezyana.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomTaskScheduler {

    private final TaskRepository taskRepository;

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void activateBlockedTasks() {
        List<Task> dueTasks = taskRepository.findBlockedTasksDue(LocalDateTime.now());

        if (dueTasks.isEmpty()) {
            return;
        }

        log.info("Activating {} blocked tasks", dueTasks.size());
        dueTasks.forEach(task -> task.setStatus(TaskStatus.ACTIVE));
        taskRepository.saveAll(dueTasks);
    }
}
