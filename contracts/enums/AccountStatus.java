package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a client account (A-02). Matches {@code docs/status-lifecycles.md}.
 *
 * <p>Reason required for moves to {@link #ON_HOLD}, to {@link #CLOSED} and
 * {@code CLOSED -> LEAD}. Moves to {@link #CLOSED} and {@code CLOSED -> LEAD} are
 * limited to managers and admins. Guard (service): an account with open job orders
 * cannot be closed.
 */
public enum AccountStatus implements Lifecycle<AccountStatus> {
    /** A prospect the firm is talking to. Initial status. */
    LEAD("Lead", "Aday müşteri", Tone.NEUTRAL),
    /** A proposal has been sent and is awaiting an answer. */
    PROPOSAL_SENT("Proposal sent", "Teklif gönderildi", Tone.INFO),
    /** The client has signed and work is ongoing. */
    ACTIVE("Active", "Aktif", Tone.GOOD),
    /** Work is paused by the client or the firm. */
    ON_HOLD("On hold", "Beklemede", Tone.WARN),
    /** The relationship is closed for now; can be reopened as a lead. */
    CLOSED("Closed", "Kapalı", Tone.MUTED);

    /** Status of a newly created account. */
    public static final AccountStatus INITIAL = LEAD;

    private final String en;
    private final String tr;
    private final Tone tone;

    AccountStatus(String en, String tr, Tone tone) {
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
    public boolean requiresReason(AccountStatus target) {
        return target == ON_HOLD || target == CLOSED || (this == CLOSED && target == LEAD);
    }

    @Override
    public boolean requiresElevatedRole(AccountStatus target) {
        return target == CLOSED || (this == CLOSED && target == LEAD);
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
