package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A person's KVKK consent status changed (S-01, NFR-02). Publisher: privacy.
 * Listeners: accounts (copy of the status on the person for lists, search and badges).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who recorded it, or {@code null} when the person answered via the e-mail link
 * @param personId   the person
 * @param from       previous status
 * @param to         new status
 * @param channel    how the decision was collected; {@code null} for {@link ConsentStatus#SENT}
 */
public record ConsentStatusChanged(UUID eventId, Instant occurredAt, UUID actorId,
                                   UUID personId, ConsentStatus from, ConsentStatus to,
                                   ConsentChannel channel) implements DomainEvent {
}
