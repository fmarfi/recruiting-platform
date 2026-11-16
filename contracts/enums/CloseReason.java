package com.recruiting.platform.contracts;

/**
 * Coded reason why an application left the process (R65, since contract 1.3.0, ADR 0013). Stored
 * in {@code application.close_reason}, set on every move to {@link PipelineStage#REJECTED} (the free
 * text {@code rejection_reason} stays required) and cleared on reinstatement. Used for funnel
 * reports; never shown to the client. Since 1.5.0 funnel and rejection counts exclude
 * {@link #MOVED_TO_OTHER_SEARCH} (a move is not a rejection).
 */
public enum CloseReason {
    /** The client declined the candidate (also set by client feedback DECLINE). */
    CLIENT_DECLINED,
    /** The candidate withdrew from the process. */
    CANDIDATE_WITHDREW,
    /** The candidate is not interested in the role. */
    NOT_INTERESTED,
    /** Does not meet the must-have criteria. */
    NOT_QUALIFIED,
    /** Pay expectations do not fit the band. */
    COMPENSATION_MISMATCH,
    /** Blocked by an off-limits rule. */
    OFF_LIMITS,
    /** No answer after repeated outreach. */
    NO_RESPONSE,
    /** The candidate accepted another offer. */
    HIRED_ELSEWHERE,
    /** The search was filled, cancelled or put on hold. */
    SEARCH_CLOSED,
    /**
     * Since 1.5.0 (ADR 0015): the candidate was moved to another job order
     * ({@code POST /applications/{id}/move}); the target application links back to this one.
     * Set only by the move; a client sending it in a stage change gets 400.
     */
    MOVED_TO_OTHER_SEARCH,
    /** Anything else; the free-text reason explains (default when none is sent). */
    OTHER
}
