package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A tag was renamed, merged into another tag or deleted on every record (since contract 1.3.0,
 * ADR 0013). Publisher: accounts (after rewriting {@code account.tags}, {@code person.tags} and
 * saved views). Listeners: pipeline rewrites {@code job_order.tags} and {@code proposal.tags}
 * with one set-based statement each (idempotent: replacing or removing an absent tag is a no-op).
 *
 * @param eventId    event id
 * @param occurredAt time of the change
 * @param actorId    the manager or admin
 * @param change     RENAMED, MERGED or DELETED
 * @param tag        the old tag (lower-case)
 * @param newTag     the new or target tag; null for DELETED
 */
public record TagChanged(UUID eventId, Instant occurredAt, UUID actorId,
                         TagChangeKind change, String tag, String newTag) implements DomainEvent {
}
