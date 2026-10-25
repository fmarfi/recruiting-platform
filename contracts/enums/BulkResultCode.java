package com.recruiting.platform.contracts;

/**
 * Reason code of a SKIPPED or FAILED record in a bulk action (ADR 0010). The lifecycle codes
 * mirror {@link ErrorCode} so a bulk move fails exactly like the single {@code PATCH .../status}.
 */
public enum BulkResultCode {
    /** SKIPPED: the record already has the requested value (owner, tag, status). */
    NO_CHANGE,
    /** FAILED: no record with this id. */
    NOT_FOUND,
    /** FAILED: the role does not allow this move (manager-only transition). */
    FORBIDDEN,
    /** FAILED: the move is not in the transition table. */
    ILLEGAL_TRANSITION,
    /** FAILED: a data guard blocks the move (e.g. open job orders when closing an account). */
    TRANSITION_GUARD_FAILED,
    /** FAILED: this move needs a reason and none was given. */
    REASON_REQUIRED,
    /** FAILED: the record's state forbids the change (e.g. closed account for a new next action). */
    STATE_CONFLICT,
    /** FAILED: the record changed concurrently; retry. */
    VERSION_CONFLICT,
    /** SKIPPED (consent gate): the person has no e-mail address. */
    NO_EMAIL,
    /** SKIPPED (consent gate): consent is already given; no request needed. */
    CONSENT_GIVEN,
    /** SKIPPED (consent gate): the person refused; bulk does not re-ask. */
    CONSENT_REFUSED,
    /** SKIPPED (consent gate): the person withdrew consent; bulk does not re-ask. */
    CONSENT_WITHDRAWN,
    /** SKIPPED (consent gate): an unanswered request is younger than 7 days. */
    REQUEST_PENDING,
    /** SKIPPED (consent gate): the person is anonymised. */
    ANONYMISED,
    /** SKIPPED (consent gate): no valid basis to contact this person (contact actions other than the consent request). */
    NO_CONSENT_BASIS,
    /** FAILED: the record already has 20 tags. */
    TOO_MANY_TAGS,
    /** FAILED: unexpected error for this record; the others are not affected. */
    INTERNAL_ERROR
}
