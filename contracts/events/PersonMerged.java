package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * Two records of the same person were merged (B-04, since contract 1.2.0, ADR 0011). Every row
 * of the merged person was moved to the survivor inside the merge transaction, and the merged
 * person was deleted through JPA. Publisher: privacy (it orchestrates the merge across accounts,
 * pipeline and privacy). Listeners: none needed in Phase 1; later modules (search index, ai)
 * re-read the survivor.
 *
 * @param eventId    event id
 * @param occurredAt time of the merge
 * @param actorId    the user who merged
 * @param survivorId the person that remains
 * @param mergedId   the person that was merged away and no longer exists
 */
public record PersonMerged(UUID eventId, Instant occurredAt, UUID actorId,
                           UUID survivorId, UUID mergedId) implements DomainEvent {
}
