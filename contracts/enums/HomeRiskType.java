package com.recruiting.platform.contracts;

/**
 * Type of a Home NEEDS YOU item (`MyDay.needsYou`, since contract 1.4.0, ADR 0014). Each type has
 * one {@link #action()}. The thresholds are the named constants below; the server owns them and
 * the client never recomputes them. New types may be added in a minor version: clients show an
 * unknown type as a generic row with its refs.
 */
public enum HomeRiskType {
    /** Active application in a client-facing stage ({@code PipelineStage.sharedWithClient()}) while the person's consent is not GIVEN. Urgent from day 0. */
    CONSENT_ON_CLIENT_STAGE(HomeAction.REQUEST_CONSENT),
    /** Application in PRESENTED for at least {@link #FEEDBACK_LATE_DAYS} days with no client feedback and no non-outbound client activity on the search since. */
    FEEDBACK_LATE(HomeAction.LOG_CLIENT_CONTACT),
    /** Active application on the LONGLIST (SOURCED, SCREENED) unchanged for at least {@link #STUCK_IN_STAGE_DAYS} days. */
    STUCK_IN_STAGE(HomeAction.MOVE_STAGE),
    /** Open search with no activity and no stage change for at least {@link #SEARCH_STALLED_DAYS} days. */
    SEARCH_STALLED(HomeAction.PLAN_NEXT_ACTION),
    /** Open next action of the user overdue by at least {@link #ACTION_OVERDUE_DAYS} days (it leaves TODAY). */
    ACTION_OVERDUE(HomeAction.COMPLETE_ACTION);

    /** Days in PRESENTED without client feedback or client activity before FEEDBACK_LATE. */
    public static final int FEEDBACK_LATE_DAYS = 5;
    /** Days unchanged in a longlist stage before STUCK_IN_STAGE. */
    public static final int STUCK_IN_STAGE_DAYS = 21;
    /** Days without activity or stage change on an open search before SEARCH_STALLED. */
    public static final int SEARCH_STALLED_DAYS = 7;
    /** Days overdue before a next action moves from TODAY to NEEDS YOU. */
    public static final int ACTION_OVERDUE_DAYS = 3;
    /** Days a search may be open with nobody on the shortlist and no shortlist due date before NO_SHORTLIST. */
    public static final int NO_SHORTLIST_DAYS = 30;

    private final HomeAction action;

    HomeRiskType(HomeAction action) {
        this.action = action;
    }

    /**
     * The default action for this type. CONSENT_ON_CLIENT_STAGE offers
     * {@link HomeAction#REMOVE_FROM_SHORTLIST} instead when the consent is REFUSED or WITHDRAWN.
     */
    public HomeAction action() {
        return action;
    }
}
