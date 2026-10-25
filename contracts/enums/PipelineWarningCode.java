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
    ACTIVE_IN_OTHER_PROCESS
}
