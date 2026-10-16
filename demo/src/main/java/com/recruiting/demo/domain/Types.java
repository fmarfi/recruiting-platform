package com.recruiting.demo.domain;

/**
 * Small closed vocabularies used across the CRM that have no lifecycle of their own.
 */
public final class Types {

    private Types() {
    }

    /** How the firm knows a person in its network (requirement A-12). */
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

    /** KVKK consent status per person (requirements S-01, NFR-02). */
    public enum ConsentStatus {
        /** No information notice or consent request sent yet. */
        NOT_SENT,
        /** Information notice and consent request sent, no answer yet. */
        SENT,
        /** Explicit consent given. */
        GIVEN,
        /** Consent refused. */
        REFUSED,
        /** Consent given earlier and then withdrawn. */
        WITHDRAWN
    }

    /** Kind of logged activity (requirement A-04, quick add UX-06). */
    public enum ActivityType {
        /** Phone call. */
        CALL,
        /** Client visit in person. */
        VISIT,
        /** Meeting, online or in person. */
        MEETING,
        /** E-mail sent or received. */
        EMAIL,
        /** Free-text note. */
        NOTE
    }
}
