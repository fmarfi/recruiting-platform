package com.recruiting.platform.contracts;

/** Why a candidate would move (SX-008 field 9, since contract 1.3.0). */
public enum MotivationTag {
    /** Career growth. */
    GROWTH,
    /** Pay. */
    PAY,
    /** Stability. */
    STABILITY,
    /** Relocation. */
    RELOCATION,
    /** Something pushes them out of the current role. */
    PUSH_FACTOR
}
