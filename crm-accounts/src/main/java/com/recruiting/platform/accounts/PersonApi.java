package com.recruiting.platform.accounts;

import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Public read API of accounts for other modules (ADR 0001): summaries and existence checks. */
public interface PersonApi {

    Optional<PersonSummary> findById(UUID id);

    Map<UUID, PersonSummary> findByIds(Collection<UUID> ids);

    default boolean exists(UUID id) {
        return findById(id).isPresent();
    }

    /**
     * Candidate picker for pipeline: people not in {@code excludeIds}, candidates and executives first, then by name.
     * Anonymised people are left out.
     */
    Page<PersonSummary> suggest(String q, Collection<UUID> excludeIds, Pageable pageable);

    record PersonSummary(UUID id, String name, PersonType type, String title, String company, String email,
            UUID ownerId, boolean anonymised, String city, Instant lastContactAt, ConsentStatus consentStatus,
            RetentionStatus retentionStatus) {
    }
}
