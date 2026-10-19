package com.recruiting.platform.contracts;

/**
 * Machine-readable error codes carried in the {@code code} property of every RFC 9457
 * problem response (ADR 0006). The HTTP status is fixed per code.
 */
public enum ErrorCode {
    /** Request body or parameters failed validation; see {@code errors}. */
    VALIDATION_FAILED(400),
    /** A status change needs a reason and none was given. */
    REASON_REQUIRED(400),
    /** Not logged in or session expired. */
    UNAUTHENTICATED(401),
    /** Wrong e-mail or password. */
    BAD_CREDENTIALS(401),
    /** Logged in but the role does not allow this action. */
    FORBIDDEN(403),
    /** The record does not exist (or the caller may not know it exists). */
    NOT_FOUND(404),
    /** The status change is not in the lifecycle's transition table. */
    ILLEGAL_TRANSITION(409),
    /** The status change is in the table but a data guard blocks it. */
    TRANSITION_GUARD_FAILED(409),
    /** The record was changed by someone else; reload and retry. */
    VERSION_CONFLICT(409),
    /** A record with the same unique key exists (e.g. same e-mail, same candidate on a job). */
    DUPLICATE(409),
    /** The record is in a state that forbids this change (e.g. terms of a sent proposal). */
    STATE_CONFLICT(409),
    /** A one-time link (consent token) has expired. */
    GONE(410),
    /** Upload too large. */
    PAYLOAD_TOO_LARGE(413),
    /** Unsupported file type. */
    UNSUPPORTED_MEDIA_TYPE(415),
    /** Rate limit exceeded (login, public consent link); retry later. */
    TOO_MANY_REQUESTS(429),
    /** Unexpected server error. */
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * Returns the HTTP status code that goes with this error.
     *
     * @return the HTTP status code
     */
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * Returns the problem {@code type} URI for this code.
     *
     * @return e.g. {@code urn:recruiting:problem:illegal-transition}
     */
    public String typeUri() {
        return "urn:recruiting:problem:" + name().toLowerCase().replace('_', '-');
    }
}
