package com.recruiting.platform.contracts;

/**
 * Non-blocking warning returned when a person is added to a job order or presented
 * ({@code Application.warnings}, since contract 1.2.0, ADR 0011). Blocking checks are errors
 * instead ({@link ErrorCode#OFF_LIMITS}, {@link ErrorCode#CONSENT_REQUIRED}).
 */
public enum PipelineWarningCode {
    /** The person was already presented to this account for another job order. */
    PREVIOUSLY_PRESENTED_TO_ACCOUNT,
    /** The person is in another job order's pipeline that is not PLACED or REJECTED. */
    ACTIVE_IN_OTHER_PROCESS,
    /** The shortlist has more than 7 people after this move (since 1.3.0, SX-014). */
    SHORTLIST_LONG,
    /**
     * Since 1.5.0 (ADR 0015): this placement filled the last opening ({@code remainingOpenings} is
     * now 0). The job order status does not change by itself; the UI offers the move to FILLED.
     */
    ALL_OPENINGS_FILLED,
    /** Since 1.5.0: more openings are filled than {@code openings}; raise openings or check the placements. */
    OPENINGS_EXCEEDED
}
