package com.recruiting.platform.contracts;

/**
 * Start point of the retention period (NFR-03, S-03). Open question to the client
 * ("counted from upload, last contact or last consent"); the setting makes all three possible.
 * ASSUMPTION (ADR 0011): the default is {@link #FROM_LAST_CONSENT}, the Product Plan wording.
 */
public enum RetentionBasis {
    /** Counted from the date the person was added. */
    FROM_CREATED,
    /** Counted from the last contact, or the creation date if there was none. */
    FROM_LAST_CONTACT,
    /**
     * Counted from the last GIVEN consent decision (initial or renewal), or the creation date if
     * consent was never given (since contract 1.2.0).
     */
    FROM_LAST_CONSENT
}
