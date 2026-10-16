package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A job order moved to another status (A-06). Publisher: pipeline. Listeners: accounts
 * (open job count when {@code from.isOpen() != to.isOpen()}), pipeline (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    user who changed it
 * @param jobOrderId the job order
 * @param accountId  its account
 * @param from       previous status
 * @param to         new status
 * @param reason     reason given, {@code null} when not required and not given
 */
public record JobOrderStatusChanged(UUID eventId, Instant occurredAt, UUID actorId,
                                    UUID jobOrderId, UUID accountId, JobOrderStatus from,
                                    JobOrderStatus to, String reason) implements DomainEvent {
}
