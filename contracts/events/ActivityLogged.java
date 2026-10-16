package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A call, visit, meeting, e-mail or note was logged (A-04, UX-06). Publisher:
 * pipeline. Listeners: accounts (account last-activity time; person last-contact date
 * when {@code type.countsAsContact()}), privacy (retention basis when it is
 * {@link RetentionBasis#FROM_LAST_CONTACT}), pipeline (timeline).
 *
 * @param eventId    event id
 * @param occurredAt time the activity was logged
 * @param actorId    user who logged it
 * @param activityId the activity
 * @param type       kind of activity
 * @param activityAt when the call, visit or meeting took place
 * @param accountId  related account, or {@code null}
 * @param personId   related person, or {@code null}
 * @param jobOrderId related job order, or {@code null}
 */
public record ActivityLogged(UUID eventId, Instant occurredAt, UUID actorId,
                             UUID activityId, ActivityType type, Instant activityAt,
                             UUID accountId, UUID personId,
                             UUID jobOrderId) implements DomainEvent {
}
