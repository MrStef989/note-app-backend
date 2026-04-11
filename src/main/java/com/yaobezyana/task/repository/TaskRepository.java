package com.yaobezyana.task.repository;

import com.yaobezyana.task.entity.Task;
import com.yaobezyana.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.inbox = true AND t.status <> :status")
    List<Task> findInboxTasks(@Param("userId") Long userId, @Param("status") TaskStatus excludedStatus);

    @Query("SELECT t FROM Task t WHERE t.status = 'BLOCKED' AND t.dueDate <= :now")
    List<Task> findBlockedTasksDue(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Task t SET t.project = null WHERE t.project.id = :projectId")
    void detachTasksFromProject(@Param("projectId") Long projectId);
}
