package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A person was added to the network (A-12), by hand, as a new contact or by import.
 * Publisher: accounts. Listeners: privacy (creates the privacy record, starts the
 * retention clock and, per retention settings, queues the KVKK notice and consent
 * e-mail, S-01).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user, or {@code null} for the import job
 * @param personId   the new person
 * @param type       person type
 * @param ownerId    relationship owner
 * @param hasEmail   whether an e-mail address is known (consent e-mail possible)
 * @param importId   the import job that created the person (A-11), or {@code null}
 */
public record PersonCreated(UUID eventId, Instant occurredAt, UUID actorId,
                            UUID personId, PersonType type, UUID ownerId,
                            boolean hasEmail, UUID importId) implements DomainEvent {
}
