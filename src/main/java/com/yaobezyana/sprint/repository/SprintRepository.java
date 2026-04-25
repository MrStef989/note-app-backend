package com.yaobezyana.sprint.repository;

import com.yaobezyana.sprint.entity.Sprint;
import com.yaobezyana.sprint.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findAllByUserIdOrderByNumberDesc(Long userId);

    Optional<Sprint> findByIdAndUserId(Long id, Long userId);

    Optional<Sprint> findFirstByUserIdAndStatus(Long userId, SprintStatus status);

    boolean existsByUserIdAndStatus(Long userId, SprintStatus status);

    @Query("SELECT COALESCE(MAX(s.number), 0) FROM Sprint s WHERE s.user.id = :userId")
    int findMaxNumberByUserId(@Param("userId") Long userId);
}
