package com.recruiting.demo.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a job order inside an account (requirement A-06).
 */
public enum JobOrderStatus implements Lifecycle<JobOrderStatus> {
    /** The role is agreed but sourcing has not started. */
    OPEN("Open", "Açık", "neutral"),
    /** Consultants are researching and sourcing candidates. */
    SOURCING("Sourcing", "Aday arama", "info"),
    /** A shortlist has been sent to the client. */
    SHORTLIST_SENT("Shortlist sent", "Kısa liste gönderildi", "progress"),
    /** The client is interviewing candidates. */
    INTERVIEWING("Interviewing", "Mülakatlar", "progress"),
    /** An offer is out to a candidate. */
    OFFER("Offer", "Teklif", "progress"),
    /** A candidate was placed; final. */
    FILLED("Filled", "Kapatıldı", "good"),
    /** The client cancelled the search; can be reopened. */
    CANCELLED("Cancelled", "İptal", "muted");

    private final String en;
    private final String tr;
    private final String tone;

    JobOrderStatus(String en, String tr, String tone) {
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

    /**
     * Tells whether the job order still needs work.
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
    public String tone() {
        return tone;
    }
}
