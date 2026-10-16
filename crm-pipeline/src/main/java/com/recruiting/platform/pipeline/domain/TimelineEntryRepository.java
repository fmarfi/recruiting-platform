package com.recruiting.platform.pipeline.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineEntryRepository extends JpaRepository<TimelineEntry, UUID> {

    Page<TimelineEntry> findByAccountIdOrderByOccurredAtDesc(UUID accountId, Pageable pageable);

    boolean existsBySourceEventId(UUID sourceEventId);
}
