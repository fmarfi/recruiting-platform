package com.recruiting.platform.contracts;

/**
 * Research status of a target company (SX-003, since contract 1.3.0). Not a lifecycle: any
 * value may follow any other, but APPROACHED runs the off-limits guard.
 */
public enum TargetCompanyStatus {
    /** Not mapped yet (default). */
    TO_MAP,
    /** People at the company identified. */
    MAPPED,
    /** People at the company contacted (off-limits guard). */
    APPROACHED,
    /** Done with this company. */
    CLOSED,
    /** The client asked not to approach this company for this search. */
    EXCLUDED
}
