package com.recruiting.platform.contracts;

/**
 * Health of a live search on Home (`MySearchHealth.flag`, since contract 1.4.0, ADR 0014). One
 * flag per search: the first that applies in declaration order, else ON_TRACK.
 */
public enum SearchHealthFlag {
    /** No activity and no stage change for {@link HomeRiskType#SEARCH_STALLED_DAYS} days. */
    STALLED,
    /** At least one application is FEEDBACK_LATE ({@link HomeRiskType#FEEDBACK_LATE_DAYS} days). */
    WAITING_ON_CLIENT,
    /** The next milestone date has passed. */
    MILESTONE_OVERDUE,
    /** Open {@link HomeRiskType#NO_SHORTLIST_DAYS} days or more, nobody on the shortlist, no shortlist due date. */
    NO_SHORTLIST,
    /** None of the above. */
    ON_TRACK
}
