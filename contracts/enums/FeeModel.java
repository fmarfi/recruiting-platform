package com.recruiting.platform.contracts;

/** How the fee is structured (PB-008, since contract 1.3.0). */
public enum FeeModel {
    /** Retained search paid in three instalments. */
    RETAINED_THIRDS,
    /** Retained search with custom instalments. */
    RETAINED_CUSTOM,
    /** Success fee: a percent of the hire's first-year cash. */
    PERCENT_FIRST_YEAR_CASH,
    /** A fixed fee. */
    FLAT_FEE,
    /** Retainer plus success fee. */
    HYBRID
}
