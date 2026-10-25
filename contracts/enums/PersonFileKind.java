package com.recruiting.platform.contracts;

/** Kind of file kept on a person (since contract 1.2.0, ADR 0011). */
public enum PersonFileKind {
    /** A CV. Versioned per person: the newest upload is the current CV; its text is searchable. */
    CV,
    /** Any other document (diploma, certificate, reference letter). Not versioned, not searched. */
    DOCUMENT
}
