package com.recruiting.platform.contracts;

/** Kind of entry in an account timeline (A-08). */
public enum TimelineKind {
    /** Account created or its details changed. */
    ACCOUNT,
    /** Account status changed. */
    ACCOUNT_STATUS,
    /** Contact added or removed. */
    CONTACT,
    /** Activity logged. */
    ACTIVITY,
    /** Proposal created or its status changed. */
    PROPOSAL,
    /** Job order created or its status changed. */
    JOB_ORDER,
    /** Candidate added to a job order or moved to another stage. */
    CANDIDATE,
    /** Account, job order or proposal archived or restored (since 1.3.0, ADR 0013). */
    ARCHIVE,
    /** Another account was merged into this one (since 1.3.0). */
    MERGE,
    /** A candidate was placed; placement created or changed (since 1.3.0). */
    PLACEMENT
}
