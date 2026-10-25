package com.recruiting.platform.contracts;

/**
 * Why a person may not be shared with a client (sharing guard, Product Plan module 9, since
 * contract 1.2.0, ADR 0011). A person is shareable only with consent GIVEN and not anonymised.
 * Carried as {@code blockingItems[].reasonCode} of a 409 {@code CONSENT_REQUIRED}.
 */
public enum ShareBlockReason {
    /** No consent request was ever sent (NOT_SENT). One-click send. */
    CONSENT_NOT_REQUESTED,
    /** A request was sent and not answered (SENT). One-click resend. */
    CONSENT_PENDING,
    /** The person refused. Only a single, deliberate request may ask again. */
    CONSENT_REFUSED,
    /** The person withdrew consent. Only a single, deliberate request may ask again. */
    CONSENT_WITHDRAWN,
    /** The person is anonymised; nothing can be shared. */
    ANONYMISED
}
