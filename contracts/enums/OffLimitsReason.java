package com.recruiting.platform.contracts;

/** Why the firm must not recruit from a company (off-limits guard, since contract 1.2.0, ADR 0011). */
public enum OffLimitsReason {
    /** The company is a client and the agreement forbids poaching from it. */
    CLIENT_AGREEMENT,
    /** A guarantee period after a placement at the company is running. */
    GUARANTEE_PERIOD,
    /** The client asked the firm not to approach this company's people. */
    CLIENT_REQUEST,
    /** Another reason, explained in the rule's note. */
    OTHER
}
