package com.recruiting.platform.contracts;

/**
 * Status of an import job (A-11). Upload validates every row (dry run); nothing is
 * written until the user commits.
 */
public enum ImportStatus {
    /** File parsed and validated; row errors listed; waiting for commit. */
    VALIDATED,
    /** Rows are being written. */
    IMPORTING,
    /** All valid rows written; final. */
    COMPLETED,
    /** File unreadable or the commit failed; nothing written; final. */
    FAILED,
    /** Discarded by the user before commit; final. */
    DISCARDED,
    /**
     * Rolled back within 30 days of completion (since 1.3.0, CO-014): every account and person it
     * created was archived with reason IMPORT_ROLLBACK, every contact it created was ended; final.
     */
    ROLLED_BACK
}
