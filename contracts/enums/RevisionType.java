package com.recruiting.platform.contracts;

/** Kind of change in an audited revision; mirrors Envers {@code RevisionType}. */
public enum RevisionType {
    /** Record created. */
    ADD,
    /** Record changed. */
    MOD,
    /** Record deleted. */
    DEL
}
