package com.yaobezyana.calendar.repository;

import com.yaobezyana.calendar.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {

    @Query("SELECT c FROM CalendarEntry c WHERE c.user.id = :userId ORDER BY CASE WHEN c.date IS NULL THEN 1 ELSE 0 END ASC, c.date ASC")
    List<CalendarEntry> findAllByUserIdOrderByDateAsc(@Param("userId") Long userId);

    Optional<CalendarEntry> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CalendarEntry c JOIN c.tasks t WHERE t.id = :taskId")
    boolean isTaskCalendarBlocked(@Param("taskId") Long taskId);
}
