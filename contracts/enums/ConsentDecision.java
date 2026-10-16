package com.recruiting.platform.contracts;

/** A decision recorded in a consent record (NFR-02). */
public enum ConsentDecision {
    /** Explicit consent given; sets the status to {@link ConsentStatus#GIVEN}. */
    GIVEN,
    /** Consent refused; sets the status to {@link ConsentStatus#REFUSED}. */
    REFUSED,
    /** Earlier consent withdrawn; sets the status to {@link ConsentStatus#WITHDRAWN}. */
    WITHDRAWN;

    /**
     * Returns the consent status this decision leads to.
     *
     * @return the resulting status
     */
    public ConsentStatus resultingStatus() {
        return switch (this) {
            case GIVEN -> ConsentStatus.GIVEN;
            case REFUSED -> ConsentStatus.REFUSED;
            case WITHDRAWN -> ConsentStatus.WITHDRAWN;
        };
    }
}
