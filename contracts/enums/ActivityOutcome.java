package com.recruiting.platform.contracts;

/** Result of a call, meeting or e-mail (R70, SX-030 quick log, since contract 1.3.0). */
public enum ActivityOutcome {
    /** Spoke with the person. */
    CONNECTED,
    /** No answer. */
    NO_ANSWER,
    /** Left a voicemail or message. */
    LEFT_MESSAGE,
    /** Asked to be called back later. */
    CALLBACK_REQUESTED,
    /** Interested in the role or proposal. */
    INTERESTED,
    /** Not interested. */
    NOT_INTERESTED,
    /** A meeting or interview was booked. */
    MEETING_BOOKED,
    /** Wrong number or wrong person. */
    WRONG_CONTACT,
    /** Meeting held or e-mail exchanged as planned. */
    COMPLETED
}
