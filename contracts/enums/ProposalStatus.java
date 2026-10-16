package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a commercial proposal (A-05). Matches {@code docs/status-lifecycles.md}.
 *
 * <p>Reason required for moves to {@link #REJECTED}. Guard (service): title and terms
 * are read-only once the proposal is no longer {@link #DRAFT}.
 */
public enum ProposalStatus implements Lifecycle<ProposalStatus> {
    /** Being written. Initial status. */
    DRAFT("Draft", "Taslak", Tone.NEUTRAL),
    /** Sent to the client. */
    SENT("Sent", "Gönderildi", Tone.INFO),
    /** Accepted by the client; final. */
    ACCEPTED("Accepted", "Kabul edildi", Tone.GOOD),
    /** Rejected by the client; can be reworked as a new draft. */
    REJECTED("Rejected", "Reddedildi", Tone.BAD);

    /** Status of a newly created proposal. */
    public static final ProposalStatus INITIAL = DRAFT;

    private final String en;
    private final String tr;
    private final Tone tone;

    ProposalStatus(String en, String tr, Tone tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
    }

    @Override
    public Set<ProposalStatus> next() {
        return switch (this) {
            case DRAFT -> EnumSet.of(SENT);
            case SENT -> EnumSet.of(ACCEPTED, REJECTED, DRAFT);
            case ACCEPTED -> EnumSet.noneOf(ProposalStatus.class);
            case REJECTED -> EnumSet.of(DRAFT);
        };
    }

    @Override
    public boolean requiresReason(ProposalStatus target) {
        return target == REJECTED;
    }

    /**
     * Tells whether title and terms may still be edited.
     *
     * @return {@code true} only for {@link #DRAFT}
     */
    public boolean termsEditable() {
        return this == DRAFT;
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
