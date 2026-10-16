package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * An account moved to another status (A-02). Publisher: accounts. Listeners: pipeline
 * (timeline entry with from, to and reason).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who changed it
 * @param accountId  the account
 * @param from       previous status
 * @param to         new status
 * @param reason     reason given, {@code null} when not required and not given
 */
public record AccountStatusChanged(UUID eventId, Instant occurredAt, UUID actorId,
                                   UUID accountId, AccountStatus from, AccountStatus to,
                                   String reason) implements DomainEvent {
}
