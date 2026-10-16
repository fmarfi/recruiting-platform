package com.recruiting.demo.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a commercial proposal to a client (requirement A-05).
 */
public enum ProposalStatus implements Lifecycle<ProposalStatus> {
    /** Being written. */
    DRAFT("Draft", "Taslak", "neutral"),
    /** Sent to the client. */
    SENT("Sent", "Gönderildi", "info"),
    /** Accepted by the client; final. */
    ACCEPTED("Accepted", "Kabul edildi", "good"),
    /** Rejected by the client; can be reworked as a new draft. */
    REJECTED("Rejected", "Reddedildi", "bad");

    private final String en;
    private final String tr;
    private final String tone;

    ProposalStatus(String en, String tr, String tone) {
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
