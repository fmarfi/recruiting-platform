package com.recruiting.platform.contracts;

/** Where a search's next milestone on Home comes from (`SearchMilestone.source`, since contract 1.4.0, ADR 0014). */
public enum MilestoneSource {
    /** Earliest open next action on the job order ({@code next_action.due_on}; its title is shown). */
    NEXT_ACTION,
    /** {@code search_brief.longlist_due_on}, while the job order is OPEN. */
    LONGLIST_DUE,
    /** {@code search_brief.shortlist_due_on}, while the job order is OPEN or SOURCING. */
    SHORTLIST_DUE,
    /** {@code search_brief.target_start_on}, while the job order is open. */
    TARGET_START
}
