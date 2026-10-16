package com.recruiting.platform.contracts;

/** What the retention run does when a person's period has ended (S-03). */
public enum RetentionAction {
    /** Replace personal fields with placeholders and keep the record (default). */
    ANONYMISE,
    /** Delete the person and their personal data. */
    DELETE
}
