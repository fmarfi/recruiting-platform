package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a data-subject request (S-04). KVKK art. 13 gives the controller 30
 * days to answer, counted from receipt. Reason required for {@link #REJECTED}. Only
 * managers and admins may change the status (enforced on the endpoint).
 */
public enum DataSubjectRequestStatus implements Lifecycle<DataSubjectRequestStatus> {
    /** Logged, not yet handled. Initial status. */
    RECEIVED("Received", "Alındı", Tone.NEUTRAL),
    /** Being handled. */
    IN_PROGRESS("In progress", "İşlemde", Tone.PROGRESS),
    /** Answered and carried out; final. */
    COMPLETED("Completed", "Tamamlandı", Tone.GOOD),
    /** Refused with a written reason; final. */
    REJECTED("Rejected", "Reddedildi", Tone.BAD);

    /** Status of a newly logged request. */
    public static final DataSubjectRequestStatus INITIAL = RECEIVED;

    private final String en;
    private final String tr;
    private final Tone tone;

    DataSubjectRequestStatus(String en, String tr, Tone tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
    }

    @Override
    public Set<DataSubjectRequestStatus> next() {
        return switch (this) {
            case RECEIVED -> EnumSet.of(IN_PROGRESS, REJECTED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED, REJECTED);
            case COMPLETED, REJECTED -> EnumSet.noneOf(DataSubjectRequestStatus.class);
        };
    }

    @Override
    public boolean requiresReason(DataSubjectRequestStatus target) {
        return target == REJECTED;
    }

    @Override
    public String labelEn() {
        return en;
    }

    @Override
    public String labelTr() {
        return tr;
    }

    @Override
    public Tone tone() {
        return tone;
    }
}
