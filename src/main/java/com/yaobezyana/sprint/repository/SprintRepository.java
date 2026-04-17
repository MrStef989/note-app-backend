package com.yaobezyana.sprint.repository;

import com.yaobezyana.sprint.entity.Sprint;
import com.yaobezyana.sprint.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Sprint> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndStatus(Long userId, SprintStatus status);
}
