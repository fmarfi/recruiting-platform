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
    OFF_LIMITS_RULE
}
