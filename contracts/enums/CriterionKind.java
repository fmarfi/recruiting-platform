package com.recruiting.platform.contracts;

/** Kind of a search brief criterion (SX-001/017, since contract 1.3.0). */
public enum CriterionKind {
    /** Must-have; becomes a scoring row on evaluations. */
    MUST_HAVE,
    /** Nice-to-have; also scorable. */
    NICE_TO_HAVE
}
