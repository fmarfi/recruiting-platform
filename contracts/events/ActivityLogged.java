package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A call, visit, meeting, e-mail or note was logged (A-04, UX-06). Publisher:
 * pipeline. Listeners: accounts (account last-activity time; person last-contact date
 * when {@code type.countsAsContact()}, for {@code personId} and, since contract 1.3.0, every
 * attendee), privacy (retention basis when it is {@link RetentionBasis#FROM_LAST_CONTACT}, for the
 * same people), pipeline (timeline).
 *
 * @param eventId           event id
 * @param occurredAt        time the activity was logged
 * @param actorId           user who logged it
 * @param activityId        the activity
 * @param type              kind of activity
 * @param activityAt        when the call, visit or meeting took place
 * @param accountId         related account, or {@code null}
 * @param personId          related person, or {@code null}
 * @param jobOrderId        related job order, or {@code null}
 * @param attendeePersonIds other people present (since 1.3.0, R70); never {@code null}, empty
 *                          when none; never contains {@code personId}
 */
public record ActivityLogged(UUID eventId, Instant occurredAt, UUID actorId,
                             UUID activityId, ActivityType type, Instant activityAt,
                             UUID accountId, UUID personId,
                             UUID jobOrderId, List<UUID> attendeePersonIds) implements DomainEvent {

    /** Copies the list and turns {@code null} into an empty list. */
    public ActivityLogged {
        attendeePersonIds = attendeePersonIds == null ? List.of() : List.copyOf(attendeePersonIds);
    }

    /**
     * The 1.2 constructor, kept so existing publishers compile: no attendees.
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
    public ActivityLogged(UUID eventId, Instant occurredAt, UUID actorId,
                          UUID activityId, ActivityType type, Instant activityAt,
                          UUID accountId, UUID personId, UUID jobOrderId) {
        this(eventId, occurredAt, actorId, activityId, type, activityAt, accountId, personId,
                jobOrderId, List.of());
    }
}
