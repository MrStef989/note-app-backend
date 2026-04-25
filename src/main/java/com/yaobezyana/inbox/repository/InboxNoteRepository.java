package com.yaobezyana.inbox.repository;

import com.yaobezyana.inbox.entity.InboxNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InboxNoteRepository extends JpaRepository<InboxNote, Long> {

    List<InboxNote> findAllByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<InboxNote> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserId(Long userId);
}
