package com.recruiting.platform.pipeline.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.recruiting.platform.contracts.TimelineKind;
import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimelineEntryRepository extends JpaRepository<TimelineEntry, UUID> {

    Page<TimelineEntry> findByAccountIdOrderByOccurredAtDesc(UUID accountId, Pageable pageable);

    boolean existsBySourceEventId(UUID sourceEventId);

    Page<TimelineEntry> findByAccountIdAndKindInOrderByOccurredAtDesc(UUID accountId, Collection<TimelineKind> kinds,
            Pageable pageable);

    /** Timeline text is free text written at event time, so it is scrubbed on anonymisation (schema.md). */
    @Modifying
    @Query("UPDATE TimelineEntry t SET t.reason = '[removed]', t.summary = '[removed]' WHERE t.personId = :personId")
    int scrubPerson(@Param("personId") UUID personId);
}
