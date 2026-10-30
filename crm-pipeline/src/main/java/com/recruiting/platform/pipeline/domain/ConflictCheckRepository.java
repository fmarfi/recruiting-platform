package com.recruiting.platform.pipeline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ConflictCheck}. */
public interface ConflictCheckRepository extends JpaRepository<ConflictCheck, UUID> {
    List<ConflictCheck> findByAccountIdOrderByCheckedAtDesc(UUID accountId);
}
