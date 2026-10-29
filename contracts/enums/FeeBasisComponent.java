package com.recruiting.platform.contracts;

/** Cash component included in the fee basis (PB-008 toggles, since contract 1.3.0). */
public enum FeeBasisComponent {
    /** Base salary. */
    BASE,
    /** Target bonus. */
    TARGET_BONUS,
    /** Guaranteed or paid bonus. */
    PAID_BONUS,
    /** Allowances. */
    ALLOWANCES,
    /** Sign-on payment. */
    SIGN_ON,
    /** Car allowance. */
    CAR,
    /** Other cash. */
    OTHER_CASH
}
