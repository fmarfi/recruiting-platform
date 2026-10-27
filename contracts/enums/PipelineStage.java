package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stage of a candidate in the pipeline of one job order (A-07). Matches
 * {@code docs/plan/status-lifecycles.md}.
 *
 * <p>Rules: an active candidate may move forward to any later active stage, back one
 * stage, or to {@link #REJECTED} (reason required). {@link #PLACED} is reachable only
 * from {@link #OFFER} and is final. A rejected candidate can be put back to
 * {@link #SOURCED} or {@link #SCREENED}. Declaration order is the board column order.
 */
public enum PipelineStage implements Lifecycle<PipelineStage> {
    /** Found and added to the job order. Initial stage. */
    SOURCED("Sourced", "Bulundu", Tone.NEUTRAL),
    /** CV screened and first call done by the consultant. */
    SCREENED("Screened", "Ön görüşme", Tone.INFO),
    /** Presented to the client in a shortlist or report. */
    PRESENTED("Presented", "Müşteriye sunuldu", Tone.PROGRESS),
    /** Interviewing with the client. */
    CLIENT_INTERVIEW("Client interview", "Müşteri mülakatı", Tone.PROGRESS),
    /** Offer made by the client. */
    OFFER("Offer", "Teklif", Tone.PROGRESS),
    /** Placed with the client; final. */
    PLACED("Placed", "Yerleşti", Tone.GOOD),
    /** Out of this process, with a reason. */
    REJECTED("Rejected", "Elendi", Tone.BAD);

    /** Stage of a newly added candidate. */
    public static final PipelineStage INITIAL = SOURCED;

    private final String en;
    private final String tr;
    private final Tone tone;

    PipelineStage(String en, String tr, Tone tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
    }

    /**
     * Whether a candidate in this stage has been shown to the client (PRESENTED, CLIENT_INTERVIEW,
     * OFFER, PLACED). Since contract 1.2.0 (ADR 0011) every move <em>into</em> such a stage runs the
     * sharing guard (409 {@code CONSENT_REQUIRED}), and the first one also runs the off-limits guard
     * and writes the exposure. Forward moves may skip PRESENTED, so callers must use this method,
     * never {@code == PRESENTED}.
     *
     * @return {@code true} for the client-facing stages
     */
    public boolean sharedWithClient() {
        return this == PRESENTED || this == CLIENT_INTERVIEW || this == OFFER || this == PLACED;
    }

    @Override
    public Set<PipelineStage> next() {
        if (this == PLACED) {
            return EnumSet.noneOf(PipelineStage.class);
        }
        if (this == REJECTED) {
            return EnumSet.of(SOURCED, SCREENED);
        }
        Set<PipelineStage> next = EnumSet.of(REJECTED);
        for (PipelineStage s : values()) {
            if (s == REJECTED) {
                continue;
            }
            boolean forward = s.ordinal() > ordinal() && (s != PLACED || this == OFFER);
            boolean oneBack = s.ordinal() == ordinal() - 1;
            if (forward || oneBack) {
                next.add(s);
            }
        }
        return next;
    }

    @Override
    public boolean requiresReason(PipelineStage target) {
        return target == REJECTED;
    }

    /**
     * Tells whether the candidate is still in the process (counts as active).
     *
     * @return {@code true} unless placed or rejected
     */
    public boolean isActive() {
        return this != PLACED && this != REJECTED;
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
