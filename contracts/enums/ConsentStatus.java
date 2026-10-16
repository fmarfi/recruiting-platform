package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * KVKK explicit-consent (açık rıza) status per person (S-01, NFR-02). The information
 * notice (aydınlatma) is tracked separately by its sent date; this status covers the
 * consent step only.
 *
 * <p>Owned by the privacy module; it changes only through consent requests and
 * recorded decisions, never by a direct status edit. {@code NOT_SENT -> GIVEN} covers
 * consent collected in person, on paper or by phone. {@code SENT -> SENT} and
 * {@code GIVEN -> GIVEN} are a resent request and a renewed consent.
 */
public enum ConsentStatus implements Lifecycle<ConsentStatus> {
    /** No consent request sent yet. Initial status. */
    NOT_SENT("Not requested", "İstenmedi", Tone.NEUTRAL),
    /** Consent request sent, no answer yet. */
    SENT("Requested", "İstendi", Tone.INFO),
    /** Explicit consent given. */
    GIVEN("Given", "Onay verdi", Tone.GOOD),
    /** Consent refused. */
    REFUSED("Refused", "Reddetti", Tone.BAD),
    /** Consent given earlier and then withdrawn. */
    WITHDRAWN("Withdrawn", "Geri çekti", Tone.WARN);

    /** Status of a newly created person. */
    public static final ConsentStatus INITIAL = NOT_SENT;

    private final String en;
    private final String tr;
    private final Tone tone;

    ConsentStatus(String en, String tr, Tone tone) {
        this.en = en;
        this.tr = tr;
        this.tone = tone;
    }

    @Override
    public Set<ConsentStatus> next() {
        return switch (this) {
            case NOT_SENT -> EnumSet.of(SENT, GIVEN, REFUSED);
            case SENT -> EnumSet.of(SENT, GIVEN, REFUSED);
            case GIVEN -> EnumSet.of(GIVEN, WITHDRAWN);
            case REFUSED -> EnumSet.of(SENT, GIVEN);
            case WITHDRAWN -> EnumSet.of(SENT, GIVEN);
        };
    }

    /**
     * Tells whether the person's data may be used for matching and outreach.
     *
     * @return {@code true} only for {@link #GIVEN}
     */
    public boolean allowsProcessing() {
        return this == GIVEN;
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
