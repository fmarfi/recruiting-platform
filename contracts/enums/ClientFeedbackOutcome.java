package com.recruiting.platform.contracts;

/** The client's decision on a presented candidate in one feedback round (SX-019, since 1.3.0). */
public enum ClientFeedbackOutcome {
    /** Go to the next round. */
    ADVANCE,
    /** Undecided; keep warm. */
    HOLD,
    /** Declined; needs a {@link DeclineReason} and closes the application (stage REJECTED). */
    DECLINE
}
