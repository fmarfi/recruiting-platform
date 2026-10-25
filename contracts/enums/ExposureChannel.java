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
    SHARE_LINK
}
