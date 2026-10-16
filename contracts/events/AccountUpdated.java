package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * Account details (name, sector, owner) changed. Publisher: accounts. Listeners:
 * pipeline (timeline when the owner changes; cached account name in job summaries).
 *
 * @param eventId       event id
 * @param occurredAt    time of the change
 * @param actorId       user who changed it
 * @param accountId     the account
 * @param name          current name
 * @param ownerId       current owner
 * @param ownerChanged  whether the owner changed in this update
 */
public record AccountUpdated(UUID eventId, Instant occurredAt, UUID actorId,
                             UUID accountId, String name, UUID ownerId,
                             boolean ownerChanged) implements DomainEvent {
}
