package com.recruiting.platform.contracts;

/** Call status of a longlist entry (SX-013, since contract 1.3.0). Screened is the stage SCREENED. */
public enum OutreachStatus {
    /** Not called yet (default). */
    TO_CALL,
    /** Called, no answer yet. */
    CALLED,
    /** Interested in the role. */
    INTERESTED,
    /** Not interested. */
    NOT_INTERESTED,
    /** Not now; try again later. */
    NOT_NOW
}
