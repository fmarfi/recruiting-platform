package com.recruiting.platform.contracts;

import java.util.Map;
import java.util.UUID;

/**
 * Lets a module move its rows from a merged account to the surviving account inside the merge
 * transaction (since contract 1.3.0, ADR 0013). {@code accounts} orchestrates
 * {@code POST /accounts/{id}/merge}: it calls every participant bean synchronously (same
 * transaction, so a failure rolls the whole merge back), then archives the merged account with
 * {@link ArchiveReason#MERGED} and publishes {@link AccountMerged}. {@code pipeline} implements
 * it for job orders, proposals, activities, next actions, timeline entries, candidate exposures,
 * agreements, fee schedule items, placements, conflict checks and target companies.
 *
 * <p>Moves go through JPA (each moved row gets an Envers revision), except
 * {@code timeline_entry} and {@code candidate_exposure}, which are append-only read models and
 * are updated with one set-based statement.
 */
public interface AccountMergeParticipant {

    /**
     * Repoints every row of this module from {@code mergedId} to {@code survivorId}.
     *
     * @param mergedId   the account being merged away
     * @param survivorId the account that remains
     * @return moved row counts per kind, e.g. {@code {"jobOrders": 2, "activities": 14}}; shown
     *         in the merge response and stored in {@code account_merge.moved}
     */
    Map<String, Integer> moveAccount(UUID mergedId, UUID survivorId);
}
