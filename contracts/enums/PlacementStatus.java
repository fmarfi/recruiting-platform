package com.recruiting.platform.contracts;

/** State of a placement (CO-021, PB-011, since contract 1.3.0). */
public enum PlacementStatus {
    /** Placed; guarantee running or not started. */
    ACTIVE,
    /** The candidate left before the guarantee ended. */
    LEFT_IN_GUARANTEE,
    /** The guarantee ended with the candidate in role (nightly job). */
    GUARANTEE_ENDED,
    /** A replacement search was opened. */
    REPLACED,
    /** The candidate withdrew after accepting. */
    CANCELLED
}
