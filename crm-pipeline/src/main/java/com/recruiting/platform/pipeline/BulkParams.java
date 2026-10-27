package com.recruiting.platform.pipeline;

import com.recruiting.platform.contracts.Language;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Validated parameters of a bulk action; only the members the action uses are set. {@code title} and {@code reason}
 * are applied to the records but never stored in the bulk audit (ADR 0010 amendment 1).
 *
 * @param ownerId  REASSIGN_OWNER: new owner; SET_NEXT_ACTION: owner of the new next actions (defaults to the actor)
 * @param status   CHANGE_STATUS: target status name, already checked to belong to the entity's lifecycle
 * @param reason   CHANGE_STATUS: reason (3..500, trimmed), optional
 * @param tag      ADD_TAG, REMOVE_TAG: trimmed, lower-case
 * @param title    SET_NEXT_ACTION: title
 * @param dueOn    SET_NEXT_ACTION: due date
 * @param language REQUEST_CONSENT: language of the e-mail (default TR)
 */
public record BulkParams(UUID ownerId, String status, String reason, String tag, String title, LocalDate dueOn,
        Language language) {
}
