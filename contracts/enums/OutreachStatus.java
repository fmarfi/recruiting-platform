package com.recruiting.platform.contracts;

/**
 * Call status of a longlist entry (SX-013, R65, since contract 1.3.0). Not a lifecycle: any value may
 * follow any other. A move to stage {@link PipelineStage#SCREENED} sets {@link #SCREENED}
 * automatically; setting {@link #SCREENED} by hand does not move the stage.
 */
public enum OutreachStatus {
    /** Not called yet (default). */
    TO_CALL,
    /** Called, no answer yet. */
    CALLED,
    /** Interested in the role. */
    INTERESTED,
    /** Not interested. */
    NOT_INTERESTED,
    /** Not now; try again later. */
    NOT_NOW,
    /** Screening call or interview done (R65). */
    SCREENED
}
