package com.recruiting.platform.contracts;

/** Channel through which a consent decision was collected (NFR-02). */
public enum ConsentChannel {
    /** The person clicked the link in the consent e-mail (S-01). */
    EMAIL_LINK,
    /** Separate unticked checkbox on the website form (S-02, Phase 2). */
    WEBSITE_FORM,
    /** Signed or stated in person, evidence noted by the user. */
    IN_PERSON,
    /** Stated by phone, evidence noted by the user. */
    PHONE,
    /** Brought over from the previous system by CSV import. */
    IMPORT
}
