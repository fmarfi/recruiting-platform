package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A CSV or Excel import was committed (A-11). One {@link AccountCreated},
 * {@link PersonCreated} or {@link ContactAdded} is published per written row as well.
 * Publisher: accounts. Listeners: privacy (summary for the consent campaign on
 * imported people, see the risk table in the requirements).
 *
 * @param eventId      event id
 * @param occurredAt   time of the commit
 * @param actorId      user who committed it
 * @param importId     the import job
 * @param kind         what was imported
 * @param importedRows number of rows written
 */
public record ImportCompleted(UUID eventId, Instant occurredAt, UUID actorId,
                              UUID importId, ImportKind kind,
                              int importedRows) implements DomainEvent {
}
