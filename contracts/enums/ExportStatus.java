package com.recruiting.platform.contracts;

/** State of a server-side list export (CO-013, since contract 1.3.0, ADR 0013). */
public enum ExportStatus {
    /** Above the approval threshold; waiting for a manager. */
    PENDING_APPROVAL,
    /** The file is built and can be downloaded until it expires (24 hours). */
    READY,
    /** A manager rejected the request; final. */
    REJECTED,
    /** The download window passed and the bytes were deleted; final. */
    EXPIRED,
    /** Building the file failed; final. */
    FAILED
}
