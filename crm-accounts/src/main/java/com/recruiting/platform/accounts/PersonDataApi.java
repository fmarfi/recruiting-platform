package com.recruiting.platform.accounts;

import java.util.UUID;

/**
 * Public write API of accounts for privacy (ADR 0001, ADR 0009 step 2). accounts changes the person row and
 * publishes {@code PersonAnonymised}; privacy and pipeline scrub their own data on that event.
 */
public interface PersonDataApi {

    /**
     * Replaces the personal fields with placeholders, removes employment history and network links, sets
     * {@code anonymised_at} and retention status ANONYMISED, then publishes {@code PersonAnonymised(deleted=false)}.
     * Repeating it on an anonymised person only repeats the event.
     *
     * @param actorId user who asked, or null for the retention run
     * @throws com.recruiting.platform.contracts.ApiException NOT_FOUND when the person does not exist
     */
    void anonymise(UUID personId, UUID actorId);

    /**
     * Deletes the person through JPA (Envers writes the id-only delete row; foreign keys cascade) and publishes
     * {@code PersonAnonymised(deleted=true)}.
     *
     * @throws com.recruiting.platform.contracts.ApiException NOT_FOUND when the person does not exist
     */
    void delete(UUID personId, UUID actorId);
}
