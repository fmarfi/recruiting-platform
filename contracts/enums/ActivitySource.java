package com.recruiting.platform.contracts;

/** How an activity entered the system (R70, since contract 1.3.0). */
public enum ActivitySource {
    /** Typed in by a user (default). */
    MANUAL,
    /** Captured from an e-mail (mail capture, R71); carries an external message id. */
    EMAIL,
    /** Logged from the call sheet of a search. */
    CALL_SHEET
}
