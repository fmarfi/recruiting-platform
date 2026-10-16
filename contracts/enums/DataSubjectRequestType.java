package com.recruiting.platform.contracts;

/** Kind of data-subject request under KVKK art. 11 (S-04). */
public enum DataSubjectRequestType {
    /** Learn whether and which data is processed; answered with the person export. */
    ACCESS,
    /** Correct inaccurate data. */
    CORRECTION,
    /** Delete or anonymise the data. */
    DELETION,
    /** Any other art. 11 right, e.g. objection; described in free text. */
    OTHER
}
