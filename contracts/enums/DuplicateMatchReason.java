package com.recruiting.platform.contracts;

/** Why two people may be the same person (B-04, since contract 1.2.0, ADR 0011). */
public enum DuplicateMatchReason {
    /** Same e-mail, case-insensitive. */
    EMAIL,
    /** Same phone number after normalising to digits with the country code (default +90). */
    PHONE,
    /** Same LinkedIn profile URL after normalising (lower case, no query, no trailing slash). */
    LINKEDIN_URL,
    /** Same name and same current company, accent- and case-insensitive. */
    NAME_COMPANY
}
