package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * One account was merged into another (since contract 1.3.0, ADR 0013). Every row of the merged
 * account was moved inside the merge transaction (accounts directly, pipeline through
 * {@link AccountMergeParticipant}); the merged account stays as an archived row with reason
 * {@link ArchiveReason#MERGED}. Publisher: accounts. Listeners: pipeline (timeline entry
 * {@link TimelineKind#MERGE} on the survivor).
 *
 * @param eventId    event id
 * @param occurredAt time of the merge
 * @param actorId    the user who merged
 * @param survivorId the account that remains
 * @param mergedId   the account that was merged away (now archived)
 */
public record AccountMerged(UUID eventId, Instant occurredAt, UUID actorId,
                            UUID survivorId, UUID mergedId) implements DomainEvent {
}
