package com.recruiting.platform.contracts;

/** Whether a candidate would move for a role (Product Plan module 5, since contract 1.2.0). */
public enum Mobility {
    /** Only roles in the current city. */
    NONE,
    /** Would relocate within the country. */
    WITHIN_COUNTRY,
    /** Would relocate abroad. */
    INTERNATIONAL
}
