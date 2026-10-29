package com.recruiting.platform.contracts;

/** The consultant's recommendation in an evaluation (SX-008, since contract 1.3.0). */
public enum Recommendation {
    /** Put on the shortlist (needs consent GIVEN, rating, summary and concerns). */
    SHORTLIST,
    /** Keep on the longlist for now. */
    HOLD,
    /** Do not present (needs a reason). */
    REJECT
}
