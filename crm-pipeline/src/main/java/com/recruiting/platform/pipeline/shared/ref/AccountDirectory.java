package com.recruiting.platform.pipeline.shared.ref;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Port to the accounts module (ADR 0001): pipeline never reads accounts tables. The adapter wraps the accounts
 * public API ({@code AccountApi}).
 */
public interface AccountDirectory {

    /**
     * Looks up accounts by id.
     *
     * @param ids account ids
     * @return the accounts that exist, by id
     */
    Map<UUID, AccountInfo> accounts(Collection<UUID> ids);

    /**
     * Counts accounts with status ACTIVE (My day statistics).
     *
     * @return the number of active accounts
     */
    long countActiveAccounts();
}
