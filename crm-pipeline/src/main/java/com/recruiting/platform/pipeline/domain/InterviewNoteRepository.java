package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data repository for {@link InterviewNote}. */
public interface InterviewNoteRepository extends JpaRepository<InterviewNote, UUID>, JpaSpecificationExecutor<InterviewNote> {
    List<InterviewNote> findByPersonIdOrderByHeldOnDesc(UUID personId);
}
