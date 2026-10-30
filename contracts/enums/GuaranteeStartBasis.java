package com.recruiting.platform.contracts;

/**
 * The date a placement guarantee counts from (R67, PB-011, since contract 1.3.0). Stored on
 * {@code agreement.guarantee_start_basis}; {@code placement.guarantee_end_on} = that date +
 * {@code guarantee_months} unless a manager overrides it.
 */
public enum GuaranteeStartBasis {
    /** The placed candidate's first working day (default). */
    START_DATE,
    /** The day the candidate accepted the offer. */
    OFFER_ACCEPTED
}
