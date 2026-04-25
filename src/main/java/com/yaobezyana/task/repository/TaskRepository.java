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

    List<Task> findByProjectId(Long projectId);

    @Query("SELECT t FROM Task t WHERE t.status = 'BLOCKED' AND t.dueDate <= :now")
    List<Task> findBlockedTasksDue(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Task t SET t.project = null WHERE t.project.id = :projectId")
    void detachTasksFromProject(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId")
    long countBySprintId(@Param("sprintId") Long sprintId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId AND t.status = :status")
    long countBySprintIdAndStatus(@Param("sprintId") Long sprintId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.sprint.id = :sprintId AND t.status IN :statuses ORDER BY t.position ASC, t.createdAt ASC")
    List<Task> findFocusTasks(@Param("sprintId") Long sprintId, @Param("statuses") List<TaskStatus> statuses);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM Task t WHERE t.sprint.id = :sprintId AND t.status = :status")
    boolean existsBySprintIdAndStatus(@Param("sprintId") Long sprintId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status <> :status")
    long countIncompleteByProjectId(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM Task t WHERE t.sprint.id = :sprintId AND t.project.id = :projectId")
    boolean existsBySprintIdAndProjectId(@Param("sprintId") Long sprintId, @Param("projectId") Long projectId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM Task t WHERE t.sprint.id = :sprintId AND t.project IS NULL")
    boolean existsRoutineTaskBySprintId(@Param("sprintId") Long sprintId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.project IS NULL AND t.status <> 'COMPLETED' AND t.sprint IS NULL")
    long countUnsprintedRoutineByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.user.id = :userId
            AND t.status = 'ACTIVE'
            AND t.sprint IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM CalendarEntry c JOIN c.tasks ct WHERE ct.id = t.id
            )
            ORDER BY t.position ASC, t.createdAt ASC
            """)
    List<Task> findSprintEligibleTasks(@Param("userId") Long userId);
}
