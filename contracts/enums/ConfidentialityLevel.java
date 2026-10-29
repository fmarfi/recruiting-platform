package com.recruiting.platform.contracts;

/**
 * How visible an account or a search is (CO-011, PB-029; since contract 1.3.0, ADR 0013). In
 * 1.3 the level is stored and shown; the visibility rule (404 for non-team users) follows in a
 * later contract.
 */
public enum ConfidentialityLevel {
    /** Visible to every user. */
    NORMAL,
    /** Visible to every user; changes and exports limited to the named team (later contract). */
    RESTRICTED,
    /** Hidden from users outside the team (e.g. replacing an incumbent; later contract). */
    CONFIDENTIAL
}
