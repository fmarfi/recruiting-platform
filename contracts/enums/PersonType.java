package com.recruiting.platform.contracts;

/** How the firm mainly relates to a person in its network (A-12). */
public enum PersonType {
    /** Senior executive the firm tracks as a potential candidate or sponsor. */
    EXECUTIVE,
    /** Contact person at a client account. */
    CLIENT_CONTACT,
    /** Someone who refers candidates or clients. */
    REFERRER,
    /** Candidate in the pool. */
    CANDIDATE
}
