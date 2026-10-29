package com.recruiting.platform.contracts;

/** Kind of generated client report (SX-021, PB-021, since contract 1.3.0, ADR 0013). */
public enum ReportKind {
    /** The ranked shortlist with evaluations (blind option). */
    SHORTLIST,
    /** Weekly or fortnightly search progress: counts only, no candidate names. */
    PROGRESS,
    /** One candidate's profile (blind option). */
    CANDIDATE_PROFILE
}
