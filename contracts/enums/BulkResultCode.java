package com.recruiting.platform.contracts;

/**
 * Reason code of a SKIPPED or FAILED record in a bulk action (ADR 0010). The lifecycle codes
 * mirror {@link ErrorCode} so a bulk move fails exactly like the single {@code PATCH .../status}.
 *
 * <p>Each code has one fixed English {@link #message()}. Since contract 1.1.1 this text is the only
 * message a bulk result may carry: it never holds names, e-mail addresses, record values or
 * exception messages, and it is not stored ({@code bulk_action_item} keeps the code only).
 */
public enum BulkResultCode {
    /** SKIPPED: the record already has the requested value (owner, tag, status). */
    NO_CHANGE("The record already has this value."),
    /** FAILED: no record with this id. */
    NOT_FOUND("No record with this id."),
    /** FAILED: the role does not allow this move (manager-only transition). */
    FORBIDDEN("Your role does not allow this change."),
    /** FAILED: the move is not in the transition table. */
    ILLEGAL_TRANSITION("This status change is not allowed."),
    /** FAILED: a data guard blocks the move (e.g. open job orders when closing an account). */
    TRANSITION_GUARD_FAILED("Other records block this status change."),
    /** FAILED: this move needs a reason and none was given. */
    REASON_REQUIRED("This status change needs a reason."),
    /** FAILED: the record's state forbids the change (e.g. closed account for a new next action). */
    STATE_CONFLICT("The record's current state does not allow this change."),
    /** FAILED: the record changed concurrently; retry. */
    VERSION_CONFLICT("The record was changed by someone else; try again."),
    /** SKIPPED (consent gate): the person has no e-mail address. */
    NO_EMAIL("The person has no e-mail address."),
    /** SKIPPED (consent gate): consent is already given; no request needed. */
    CONSENT_GIVEN("Consent is already given."),
    /** SKIPPED (consent gate): the person refused; bulk does not re-ask. */
    CONSENT_REFUSED("Consent was refused; bulk requests do not re-ask."),
    /** SKIPPED (consent gate): the person withdrew consent; bulk does not re-ask. */
    CONSENT_WITHDRAWN("Consent was withdrawn; bulk requests do not re-ask."),
    /** SKIPPED (consent gate): an unanswered request is younger than 7 days. */
    REQUEST_PENDING("A consent request was sent less than 7 days ago."),
    /** SKIPPED (consent gate): the person is anonymised. */
    ANONYMISED("The person is anonymised."),
    /** SKIPPED (consent gate): no valid basis to contact this person (contact actions other than the consent request). */
    NO_CONSENT_BASIS("There is no valid consent to contact this person."),
    /** FAILED: the record already has 20 tags. */
    TOO_MANY_TAGS("The record already has 20 tags."),
    /** FAILED: unexpected error for this record; the others are not affected. */
    INTERNAL_ERROR("Unexpected error for this record; the others are not affected.");

    private final String message;

    BulkResultCode(String message) {
        this.message = message;
    }

    /**
     * Returns the fixed English text for this code, used as {@code BulkRecordResult.message}.
     * It never contains personal data; the UI translates the code itself.
     *
     * @return the message text
     */
    public String message() {
        return message;
    }
}
