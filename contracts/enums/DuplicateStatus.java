package com.recruiting.platform.contracts;

/** State of a possible duplicate pair in the duplicate queue (B-04, since contract 1.2.0). */
public enum DuplicateStatus {
    /** Detected, not yet reviewed. */
    OPEN,
    /** The two people were merged into one. */
    MERGED,
    /** A user decided they are different people; the pair is not proposed again. */
    DISMISSED
}
