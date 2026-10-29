package com.recruiting.platform.contracts;

/** Event that makes a fee schedule item due (PB-009, CO-020, since contract 1.3.0). */
public enum FeeTrigger {
    /** The agreement is signed. */
    SIGNING,
    /** A number of days after signing (nightly job). */
    DATE_OFFSET,
    /** The first candidate of the search moved into a client-facing stage, or a shortlist report was marked sent. */
    SHORTLIST_DELIVERED,
    /** A placement was created. */
    OFFER_ACCEPTED,
    /** The placed candidate's start date is reached (nightly job). */
    START_DATE,
    /** Expenses; made due by hand. */
    EXPENSES,
    /** The search was cancelled. */
    ABORT
}
