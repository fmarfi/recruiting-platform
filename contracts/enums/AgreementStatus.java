package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * State of a fee agreement (since contract 1.3.0). A plain field, not a guarded lifecycle:
 * {@code PATCH /agreements/{id}} may set any status (data rules such as "SIGNED needs
 * {@code signedOn}" stay in the service).
 *
 * <p>Since contract 1.5.0 (ADR 0015) it implements {@link Lifecycle} only so that
 * {@code GET /reference-data} can expose labels and tones like the other statuses
 * ({@code lifecycles.agreement}). {@link #next()} therefore lists every other status, with no
 * reason and no manager-only rule, which is exactly what the PATCH accepts.
 */
public enum AgreementStatus implements Lifecycle<AgreementStatus> {
    /** Being prepared (default; may be deleted). */
    DRAFT("Draft", "Taslak", Tone.NEUTRAL),
    /** Sent to the client. */
    SENT("Sent", "Gönderildi", Tone.INFO),
    /** Signed; needs the signing date and makes SIGNING items due. */
    SIGNED("Signed", "İmzalandı", Tone.GOOD),
    /** Past its validity. */
    EXPIRED("Expired", "Süresi doldu", Tone.MUTED),
    /** Ended early. */
    TERMINATED("Terminated", "Feshedildi", Tone.BAD);

    /** Status of a new agreement. */
    public static final AgreementStatus INITIAL = DRAFT;

    private final String en;
    private final String tr;
    private final Tone tone;

    AgreementStatus(String en, String tr, Tone tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
    }

    @Override
    public Set<AgreementStatus> next() {
        Set<AgreementStatus> all = EnumSet.allOf(AgreementStatus.class);
        all.remove(this);
        return all;
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
