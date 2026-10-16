package com.recruiting.platform.contracts;

/**
 * Read access recorded in the access log (NFR-05: who viewed or exported a CV).
 * Writes are covered by Envers revisions (ADR 0005).
 */
public enum AccessAction {
    /** Person detail page opened. */
    VIEW,
    /** Person data exported (S-04 access request, NFR-11). */
    EXPORT,
    /** CV or other file downloaded (Phase 2). */
    DOWNLOAD
}
