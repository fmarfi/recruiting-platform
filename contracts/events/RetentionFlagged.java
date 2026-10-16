package com.recruiting.platform.contracts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A person's retention period ends soon or has ended (S-03). Publisher: privacy
 * (nightly retention job). Listeners: accounts (retention status on the person),
 * pipeline (next action "Renew consent" for the relationship owner, due on
 * {@code retainUntil}).
 *
 * @param eventId     event id
 * @param occurredAt  time of the run
 * @param actorId     always {@code null} (system job)
 * @param personId    the person
 * @param ownerId     the person's relationship owner, or {@code null}
 * @param retainUntil last day the data may be kept without renewed consent
 */
public record RetentionFlagged(UUID eventId, Instant occurredAt, UUID actorId,
                               UUID personId, UUID ownerId,
                               LocalDate retainUntil) implements DomainEvent {
}
