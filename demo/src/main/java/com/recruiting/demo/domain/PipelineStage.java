package com.recruiting.demo.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stage of a candidate in the pipeline of one job order (requirement A-07).
 *
 * <p>Rules: a candidate may move forward to any later stage, back one stage, or to
 * {@link #REJECTED} from any active stage (a reason is then required). A rejected
 * candidate can be put back to {@link #SOURCED} or {@link #SCREENED}. {@link #PLACED}
 * is final.
 */
public enum PipelineStage implements Lifecycle<PipelineStage> {
    /** Found and added to the job order. */
    SOURCED("Sourced", "Bulundu", "neutral"),
    /** CV screened and first call done by the consultant. */
    SCREENED("Screened", "Ön görüşme", "info"),
    /** Presented to the client in a shortlist or report. */
    PRESENTED("Presented", "Müşteriye sunuldu", "progress"),
    /** Interviewing with the client. */
    CLIENT_INTERVIEW("Client interview", "Müşteri mülakatı", "progress"),
    /** Offer made by the client. */
    OFFER("Offer", "Teklif", "progress"),
    /** Placed with the client; final. */
    PLACED("Placed", "Yerleşti", "good"),
    /** Out of this process, with a reason. */
    REJECTED("Rejected", "Elendi", "bad");

    private final String en;
    private final String tr;
    private final String tone;

    PipelineStage(String en, String tr, String tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
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
            boolean forward = s.ordinal() > ordinal() && s != REJECTED;
            boolean oneBack = s.ordinal() == ordinal() - 1;
            if (forward || oneBack) {
                next.add(s);
            }
        }
        return next;
    }

    /**
     * Tells whether moving to this stage needs a written reason.
     *
     * @return {@code true} for {@link #REJECTED}
     */
    public boolean requiresReason() {
        return this == REJECTED;
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
