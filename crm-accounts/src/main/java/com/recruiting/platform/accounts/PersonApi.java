package com.recruiting.platform.accounts;

import com.recruiting.platform.contracts.PersonType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Public read API of accounts for other modules (ADR 0001): summaries and existence checks. */
public interface PersonApi {

    Optional<PersonSummary> findById(UUID id);

    Map<UUID, PersonSummary> findByIds(Collection<UUID> ids);

    default boolean exists(UUID id) {
        return findById(id).isPresent();
    }

    record PersonSummary(UUID id, String name, PersonType type, String title, String company, String email,
            UUID ownerId, boolean anonymised) {
    }
}
