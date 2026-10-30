package com.recruiting.platform.contracts;

/** Seniority level of a person or a search, highest first (SX-025; since contract 1.3.0). */
public enum Seniority {
    /** Board member, chair. */
    BOARD,
    /** C-level (CEO, CFO, CHRO ...). */
    C_LEVEL,
    /** Vice president or director. */
    VP_DIRECTOR,
    /** Head of a function or department. */
    HEAD,
    /** Manager. */
    MANAGER,
    /** Specialist or individual contributor. */
    SPECIALIST
}
