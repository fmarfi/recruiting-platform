package com.recruiting.platform.contracts;

/** Decision recorded with a conflict check (PB-018/019, since contract 1.3.0). */
public enum ConflictDecision {
    /** No hits. */
    CLEAR,
    /** Proceed despite hits; manager override with a reason. */
    PROCEED,
    /** Do not proceed. */
    BLOCKED
}
