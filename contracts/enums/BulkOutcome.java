package com.recruiting.platform.contracts;

/** Result of a bulk action for one record (ADR 0010). Partial success is normal. */
public enum BulkOutcome {
    /** Done. */
    OK,
    /** Nothing done on purpose (no change needed, or the consent gate); see the code. */
    SKIPPED,
    /** Could not be done (not found, lifecycle rule, guard, error); see the code. */
    FAILED
}
