package com.recruiting.platform.contracts;

/**
 * The list a candidate is on within a search (SX-013/014, since contract 1.3.0, ADR 0013). Not
 * stored: derived from {@link PipelineStage#candidateList()}, so the stage enum and its
 * transition table stay unchanged.
 */
public enum CandidateList {
    /** Research and screening: SOURCED, SCREENED. */
    LONGLIST,
    /** Presented to the client: PRESENTED, CLIENT_INTERVIEW, OFFER, PLACED. */
    SHORTLIST
}
