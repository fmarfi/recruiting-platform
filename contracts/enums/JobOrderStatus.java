package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a job order inside an account (A-06). Matches
 * {@code docs/status-lifecycles.md}.
 *
 * <p>Reason required for moves to {@link #CANCELLED}. {@code CANCELLED -> OPEN} is
 * limited to managers and admins. Guards (service): {@code OFFER -> FILLED} needs at
 * least one application in {@link PipelineStage#PLACED}; a closed account accepts no
 * new job orders; a job order that is not open accepts no new candidates.
 */
public enum JobOrderStatus implements Lifecycle<JobOrderStatus> {
    /** The role is agreed but sourcing has not started. Initial status. */
    OPEN("Open", "Açık", Tone.NEUTRAL),
    /** Consultants are researching and sourcing candidates. */
    SOURCING("Sourcing", "Aday arama", Tone.INFO),
    /** A shortlist has been sent to the client. */
    SHORTLIST_SENT("Shortlist sent", "Kısa liste gönderildi", Tone.PROGRESS),
    /** The client is interviewing candidates. */
    INTERVIEWING("Interviewing", "Mülakatlar", Tone.PROGRESS),
    /** An offer is out to a candidate. */
    OFFER("Offer", "Teklif", Tone.PROGRESS),
    /** A candidate was placed; final. */
    FILLED("Filled", "Kapatıldı", Tone.GOOD),
    /** The client cancelled the search; can be reopened by a manager. */
    CANCELLED("Cancelled", "İptal", Tone.MUTED);

    /** Status of a newly created job order. */
    public static final JobOrderStatus INITIAL = OPEN;

    private final String en;
    private final String tr;
    private final Tone tone;

    JobOrderStatus(String en, String tr, Tone tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
    }

    @Override
    public Set<JobOrderStatus> next() {
        return switch (this) {
            case OPEN -> EnumSet.of(SOURCING, CANCELLED);
            case SOURCING -> EnumSet.of(SHORTLIST_SENT, CANCELLED);
            case SHORTLIST_SENT -> EnumSet.of(INTERVIEWING, SOURCING, CANCELLED);
            case INTERVIEWING -> EnumSet.of(OFFER, SOURCING, CANCELLED);
            case OFFER -> EnumSet.of(FILLED, INTERVIEWING, CANCELLED);
            case FILLED -> EnumSet.noneOf(JobOrderStatus.class);
            case CANCELLED -> EnumSet.of(OPEN);
        };
    }

    @Override
    public boolean requiresReason(JobOrderStatus target) {
        return target == CANCELLED;
    }

    @Override
    public boolean requiresElevatedRole(JobOrderStatus target) {
        return this == CANCELLED && target == OPEN;
    }

    /**
     * Tells whether the job order still needs work (counts as an open job).
     *
     * @return {@code true} unless the job is filled or cancelled
     */
    public boolean isOpen() {
        return this != FILLED && this != CANCELLED;
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
