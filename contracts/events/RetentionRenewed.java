package com.recruiting.platform.contracts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A person's retention period was restarted, usually because consent was given again through a
 * renewal link (S-03, since contract 1.2.0, ADR 0011). Publisher: privacy. Listeners: accounts
 * ({@code person.retention_status = ACTIVE}), pipeline (mark the open "Renew consent" next action
 * created from {@link RetentionFlagged} for this person as done).
 *
 * @param eventId     event id
 * @param occurredAt  time of the renewal
 * @param actorId     user, or {@code null} when the person used the link
 * @param personId    the person
 * @param retainUntil the new last day of the retention period
 */
public record RetentionRenewed(UUID eventId, Instant occurredAt, UUID actorId,
                               UUID personId, LocalDate retainUntil) implements DomainEvent {
}
