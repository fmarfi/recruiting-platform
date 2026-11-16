package com.recruiting.platform.contracts;

/**
 * How a person was shown to a client account (exposure history, since contract 1.2.0, ADR 0011).
 * {@code REPORT} and {@code SHARE_LINK} are reserved for Wave 2d (reports and share links).
 */
public enum ExposureChannel {
    /** The application moved to PRESENTED on the job order's pipeline. */
    PIPELINE,
    /** Included in a candidate report or shortlist PDF sent to the client (Wave 2d). */
    REPORT,
    /** Included in a shortlist share link (Wave 2d). */
    SHARE_LINK,
    /**
     * CV pushed: the person's CV was sent to the client account <em>without</em> a job order, logged
     * as an activity with {@code cvPush = true} (since contract 1.5.0, ADR 0015). The exposure row has
     * {@code activity_id} set and {@code job_order_id} null. Counted as "CVs pushed" on Home.
     */
    CV_PUSH
}
