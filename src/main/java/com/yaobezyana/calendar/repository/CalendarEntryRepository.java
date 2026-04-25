package com.yaobezyana.calendar.repository;

import com.yaobezyana.calendar.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {

    List<CalendarEntry> findAllByUserIdOrderByDateAsc(Long userId);

    Optional<CalendarEntry> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM CalendarEntry c JOIN c.tasks t WHERE t.id = :taskId")
    boolean isTaskCalendarBlocked(@Param("taskId") Long taskId);
}
