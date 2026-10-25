package com.recruiting.platform.contracts;

/** Why a consent e-mail was sent (S-01, S-03, since contract 1.2.0, ADR 0011). */
public enum ConsentRequestPurpose {
    /** First request (or a resend) of the information notice and explicit consent. */
    INITIAL,
    /**
     * Renewal before the retention period ends (reminders at 60 and 14 days by default). Giving
     * consent through it restarts the retention clock. Allowed while the status is GIVEN.
     */
    RENEWAL
}
