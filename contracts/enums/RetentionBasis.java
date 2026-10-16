package com.recruiting.platform.contracts;

/**
 * Start point of the retention period (NFR-03). Open question to the client
 * ("counted from upload or from last contact"); the setting makes both possible.
 */
public enum RetentionBasis {
    /** Counted from the date the person was added. */
    FROM_CREATED,
    /** Counted from the last contact, or the creation date if there was none. */
    FROM_LAST_CONTACT
}
