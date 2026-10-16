package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * An account was created (A-01). Publisher: accounts. Listeners: pipeline (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user, or {@code null} for the import job
 * @param accountId  the new account
 * @param name       account name
 * @param ownerId    responsible consultant
 * @param status     initial status
 */
public record AccountCreated(UUID eventId, Instant occurredAt, UUID actorId,
                             UUID accountId, String name, UUID ownerId,
                             AccountStatus status) implements DomainEvent {
}
