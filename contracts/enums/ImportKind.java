package com.recruiting.platform.contracts;

/** What a CSV or Excel import creates (A-11). */
public enum ImportKind {
    /** Client accounts. */
    ACCOUNTS,
    /** People linked to an account as contacts (account matched by name). */
    CONTACTS,
    /** Network people without an account link. */
    PEOPLE
}
