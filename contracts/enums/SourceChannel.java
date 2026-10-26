package com.recruiting.platform.contracts;

/**
 * How a candidate reached the firm (B-03, since contract 1.2.0, ADR 0011). Stored on
 * {@code person.source_channel}; it is not personal data and survives anonymisation, so pool
 * statistics per channel stay correct. No LinkedIn integration: the LinkedIn values only record
 * where the consultant found the person.
 */
public enum SourceChannel {
    /** Applied to a job ad the firm posted on LinkedIn. */
    LINKEDIN_AD,
    /** Contacted the firm, or was contacted, by a LinkedIn message. */
    LINKEDIN_DIRECT,
    /** Came through the firm's website (the web form itself is not built, see ADR 0011). */
    WEBSITE,
    /** Recommended by someone the firm knows. */
    REFERRAL,
    /** Already in the firm's pool or the old system before the platform. */
    EXISTING_POOL,
    /** Sent a CV or details by e-mail. */
    EMAIL,
    /** Entered by a consultant's CV upload with no other known channel. */
    CV_UPLOAD
}
