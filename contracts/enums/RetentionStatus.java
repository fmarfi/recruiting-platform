package com.recruiting.platform.contracts;

/** Retention state of a person's data (S-03, NFR-03). */
public enum RetentionStatus {
    /** Within the retention period. */
    ACTIVE,
    /** The period ends within the warning window or has ended; waiting for renewal or the retention run. */
    FLAGGED,
    /** Personal fields replaced by placeholders; the record stays for statistics. */
    ANONYMISED
}
