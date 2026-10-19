package com.recruiting.platform.privacy.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConsentRequestRepository
        extends JpaRepository<ConsentRequest, UUID>, JpaSpecificationExecutor<ConsentRequest> {

    Optional<ConsentRequest> findByTokenHash(String tokenHash);

    List<ConsentRequest> findByPersonId(UUID personId);

    Optional<ConsentRequest> findFirstByPersonIdOrderByQueuedAtDesc(UUID personId);

    /** True when a request to this person has been waiting for an answer since after the given time. */
    boolean existsByPersonIdAndRespondedAtIsNullAndQueuedAtAfter(UUID personId, Instant after);
}
