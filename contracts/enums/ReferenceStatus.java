package com.recruiting.platform.contracts;

/** State of one reference check (SX-010, since contract 1.3.0). */
public enum ReferenceStatus {
    /** Planned; the candidate's permission to call the referee is still needed. */
    TO_REQUEST,
    /** Referee contacted. */
    REQUESTED,
    /** Reference taken. */
    RECEIVED,
    /** The referee declined or could not be reached. */
    DECLINED
}
