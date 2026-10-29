package com.recruiting.platform.contracts;

/** What a conflict check found (PB-018, since contract 1.3.0). */
public enum ConflictHitKind {
    /** The company or a group company is an active client. */
    CURRENT_CLIENT,
    /** A placement guarantee is running at the company or its group. */
    GUARANTEE_PERIOD,
    /** An active off-limits rule covers the company. */
    OFF_LIMITS_RULE,
    /** Someone we placed works there now (counted, never named). */
    PLACED_CANDIDATE,
    /** An open search for a similar role runs for another client. */
    COMPETING_SEARCH
}
