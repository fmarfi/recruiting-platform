package com.recruiting.platform.contracts;

/** Why a client declined a candidate (SX-019 picklist, since contract 1.3.0). */
public enum DeclineReason {
    /** Experience or skills do not fit. */
    EXPERIENCE,
    /** Compensation expectations too high. */
    COMPENSATION,
    /** Culture or team fit. */
    CULTURE_FIT,
    /** Location or mobility. */
    LOCATION,
    /** Timing or notice period. */
    TIMING,
    /** Another candidate was preferred. */
    OTHER_CANDIDATE,
    /** Another reason, in the text. */
    OTHER
}
