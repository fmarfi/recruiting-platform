package com.recruiting.platform.pipeline.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link SearchBrief}. */
public interface SearchBriefRepository extends JpaRepository<SearchBrief, UUID> {
}
