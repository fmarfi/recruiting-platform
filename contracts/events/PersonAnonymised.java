package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A person's personal fields were anonymised or the person was deleted (S-03, S-04).
 * Publisher: accounts (after privacy called {@code PersonDataApi}). Listeners:
 * pipeline (scrub free text that names the person: rejection reasons, activity notes,
 * next actions), privacy (close the retention record).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user, or {@code null} for the retention run
 * @param personId   the person
 * @param deleted    {@code true} if the row was deleted, {@code false} if anonymised
 */
public record PersonAnonymised(UUID eventId, Instant occurredAt, UUID actorId,
                               UUID personId, boolean deleted) implements DomainEvent {
}
