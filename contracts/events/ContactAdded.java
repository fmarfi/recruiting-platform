package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A person was linked to an account as a contact (A-03). Publisher: accounts.
 * Listeners: pipeline (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user, or {@code null} for the import job
 * @param contactId  the contact link
 * @param accountId  the account
 * @param personId   the person
 * @param role       role at the client
 */
public record ContactAdded(UUID eventId, Instant occurredAt, UUID actorId,
                           UUID contactId, UUID accountId, UUID personId,
                           String role) implements DomainEvent {
}
