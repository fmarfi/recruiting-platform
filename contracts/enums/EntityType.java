package com.recruiting.platform.contracts;

/** Types of record referred to by the timeline, events and the audit log. */
public enum EntityType {
    ACCOUNT,
    PERSON,
    CONTACT,
    NETWORK_LINK,
    JOB_ORDER,
    APPLICATION,
    PROPOSAL,
    ACTIVITY,
    NEXT_ACTION,
    USER,
    CONSENT_RECORD,
    CONSENT_REQUEST,
    PRIVACY_NOTICE,
    DATA_SUBJECT_REQUEST,
    IMPORT_JOB,
    /** A CV or document of a person (since 1.2.0). */
    PERSON_FILE,
    /** An uploaded CV waiting in the intake queue (since 1.2.0). */
    CV_INTAKE_ITEM,
    /** An off-limits company rule (since 1.2.0). */
    OFF_LIMITS_RULE,
    /** A tag definition (colour, talent pool flag; since 1.3.0). */
    TAG,
    /** A candidate evaluation (scorecard) per person and search (since 1.3.0). */
    EVALUATION,
    /** Interview notes (since 1.3.0). */
    INTERVIEW_NOTE,
    /** A reference check (since 1.3.0). */
    CANDIDATE_REFERENCE,
    /** Client feedback on a shortlisted candidate (since 1.3.0). */
    CLIENT_FEEDBACK,
    /** A target company of a search (since 1.3.0). */
    TARGET_COMPANY,
    /** A fee agreement: terms of business or engagement letter (since 1.3.0). */
    AGREEMENT,
    /** One instalment or fee item of an agreement (since 1.3.0). */
    FEE_SCHEDULE_ITEM,
    /** A placement with guarantee (since 1.3.0). */
    PLACEMENT,
    /** A conflict check before a proposal or search (since 1.3.0). */
    CONFLICT_CHECK,
    /** A server-side list export (since 1.3.0). */
    EXPORT_JOB,
    /** A generated client report (shortlist, progress, profile; since 1.3.0). */
    CLIENT_REPORT
}
