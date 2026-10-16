package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * Common header of every domain event published between modules
 * ({@code contracts/events.md}, ADR 0001). Events are immutable records, published
 * with Spring's {@code ApplicationEventPublisher} inside the writing transaction and
 * consumed with {@code @ApplicationModuleListener} (after commit, at least once), so
 * listeners must be idempotent on {@link #eventId()}.
 */
public interface DomainEvent {

    /**
     * Returns the unique id of this event, used by listeners for de-duplication.
     *
     * @return the event id (UUID v7)
     */
    UUID eventId();

    /**
     * Returns when the change happened.
     *
     * @return the time of the change, UTC
     */
    Instant occurredAt();

    /**
     * Returns the user who caused the change.
     *
     * @return the user id, or {@code null} for system jobs (retention run, import job, mail job)
     */
    UUID actorId();
}
