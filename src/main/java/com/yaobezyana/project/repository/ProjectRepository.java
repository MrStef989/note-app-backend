package com.yaobezyana.project.repository;

import com.yaobezyana.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByUserId(Long userId);

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    List<Project> findAllBySprintId(Long sprintId);

    List<Project> findAllBySprintIdAndUserId(Long sprintId, Long userId);
}
