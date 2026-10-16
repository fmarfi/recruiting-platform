package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A person's details changed. Publisher: accounts. Listeners: privacy (when the e-mail
 * was added or changed, a pending consent request may be sent to the new address).
 *
 * @param eventId      event id
 * @param occurredAt   time of the change
 * @param actorId      user who changed it
 * @param personId     the person
 * @param type         current type
 * @param emailChanged whether the e-mail address was added or changed
 */
public record PersonUpdated(UUID eventId, Instant occurredAt, UUID actorId,
                            UUID personId, PersonType type,
                            boolean emailChanged) implements DomainEvent {
}
