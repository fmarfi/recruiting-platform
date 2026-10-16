package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A contact link was removed from an account; the person stays in the network.
 * Publisher: accounts. Listeners: pipeline (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who removed it
 * @param contactId  the contact that was ended ({@code ended_on} set; the row is kept)
 * @param accountId  the account
 * @param personId   the person
 */
public record ContactRemoved(UUID eventId, Instant occurredAt, UUID actorId,
                             UUID contactId, UUID accountId,
                             UUID personId) implements DomainEvent {
}
