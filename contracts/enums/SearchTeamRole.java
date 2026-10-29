package com.recruiting.platform.contracts;

/** Role of a user in a search team (since contract 1.3.0); the job order owner is always LEAD. */
public enum SearchTeamRole {
    /** Lead consultant or partner. */
    LEAD,
    /** Consultant. */
    CONSULTANT,
    /** Researcher. */
    RESEARCHER
}
