package com.recruiting.demo.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a client account (requirement A-02).
 *
 * <p>Demo assumption until the client confirms the statuses they use today.
 */
public enum AccountStatus implements Lifecycle<AccountStatus> {
    /** A prospect the firm is talking to. */
    LEAD("Lead", "Aday müşteri", "neutral"),
    /** A proposal has been sent and is awaiting an answer. */
    PROPOSAL_SENT("Proposal sent", "Teklif gönderildi", "info"),
    /** The client has signed and work is ongoing. */
    ACTIVE("Active", "Aktif", "good"),
    /** Work is paused by the client or the firm. */
    ON_HOLD("On hold", "Beklemede", "warn"),
    /** The relationship is closed for now; can be reopened as a lead. */
    CLOSED("Closed", "Kapalı", "muted");

    private final String en;
    private final String tr;
    private final String tone;

    AccountStatus(String en, String tr, String tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
    }

    @Override
    public Set<AccountStatus> next() {
        return switch (this) {
            case LEAD -> EnumSet.of(PROPOSAL_SENT, CLOSED);
            case PROPOSAL_SENT -> EnumSet.of(ACTIVE, ON_HOLD, LEAD, CLOSED);
            case ACTIVE -> EnumSet.of(ON_HOLD, CLOSED);
            case ON_HOLD -> EnumSet.of(ACTIVE, CLOSED);
            case CLOSED -> EnumSet.of(LEAD);
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
