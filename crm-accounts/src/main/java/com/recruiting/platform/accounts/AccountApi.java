package com.recruiting.platform.accounts;

import com.recruiting.platform.contracts.AccountStatus;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Public read API of accounts for other modules (ADR 0001): references and existence checks. */
public interface AccountApi {

    Optional<AccountSummary> findById(UUID id);

    Map<UUID, AccountSummary> findByIds(Collection<UUID> ids);

    default boolean exists(UUID id) {
        return findById(id).isPresent();
    }

    record AccountSummary(UUID id, String name, String sector, AccountStatus status, UUID ownerId) {
    }
}
