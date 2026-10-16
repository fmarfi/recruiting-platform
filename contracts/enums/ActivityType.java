package com.recruiting.platform.contracts;

/**
 * Kind of logged activity (A-04, quick add UX-06). Every type except {@link #NOTE}
 * counts as a contact and updates the person's last contact date.
 */
public enum ActivityType {
    /** Phone or video call. */
    CALL,
    /** Client visit in person. */
    VISIT,
    /** Meeting, online or in person. */
    MEETING,
    /** E-mail sent or received. */
    EMAIL,
    /** Free-text note; not a contact. */
    NOTE;

    /**
     * Tells whether this activity is a contact with the person.
     *
     * @return {@code false} only for {@link #NOTE}
     */
    public boolean countsAsContact() {
        return this != NOTE;
    }
}
