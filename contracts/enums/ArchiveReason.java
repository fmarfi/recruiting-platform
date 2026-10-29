package com.recruiting.platform.contracts;

/**
 * Why a business record (account, job order, proposal, person) was archived (soft delete, since
 * contract 1.3.0, ADR 0013). Archived records leave the lists and can be restored, except
 * {@link #MERGED}.
 */
public enum ArchiveReason {
    /** A second record of the same thing; the other one is kept. */
    DUPLICATE,
    /** Entered by mistake (wrong client, test record). */
    CREATED_BY_MISTAKE,
    /** Still correct but no longer relevant for daily work. */
    NO_LONGER_RELEVANT,
    /** The account was merged into another one ({@code merged_into_id}); cannot be restored. */
    MERGED,
    /** Archived by rolling back the import that created it. */
    IMPORT_ROLLBACK,
    /** Another reason, explained in the archive note. */
    OTHER
}
