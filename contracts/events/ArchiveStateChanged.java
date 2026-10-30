package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A business record was archived or restored (soft delete, since contract 1.3.0, ADR 0013).
 * Publisher: accounts (ACCOUNT, PERSON) or pipeline (JOB_ORDER). Listeners: pipeline
 * (timeline entry {@link TimelineKind#ARCHIVE} when {@code accountId} is set); accounts (for a
 * JOB_ORDER: upsert {@code account_open_job} with {@code open = false} while archived, so archived
 * job orders never block closing or archiving the account).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who archived or restored
 * @param entityType ACCOUNT, PERSON or JOB_ORDER (proposal archive is cut from 1.3)
 * @param entityId   the record
 * @param accountId  the account it belongs to (the account itself for ACCOUNT); null for PERSON
 * @param archived   true = archived, false = restored
 * @param reason     the archive reason; null on restore
 */
public record ArchiveStateChanged(UUID eventId, Instant occurredAt, UUID actorId,
                                  EntityType entityType, UUID entityId, UUID accountId,
                                  boolean archived, ArchiveReason reason) implements DomainEvent {
}
