package com.recruiting.platform.contracts;

import java.util.EnumSet;
import java.util.Set;

/**
 * KVKK explicit-consent (açık rıza) status per person (S-01, NFR-02). The information
 * notice (aydınlatma) is tracked separately by its sent date; this status covers the
 * consent step only.
 *
 * <p>Owned by the privacy module; it changes only through consent requests and
 * recorded decisions, never by a direct status edit. There are no self-transitions:
 * re-sending a request while {@code SENT}, and renewing a {@code GIVEN} consent, are actions
 * ({@code POST /people/{id}/consent/requests}, a new consent record), not status changes.
 *
 * <p>Triggers:
 * <ul>
 *   <li>{@code NOT_SENT -> SENT}: consent request e-mail queued (manual, auto-send or bulk).</li>
 *   <li>{@code NOT_SENT -> GIVEN}: consent collected in person, on paper or by phone, recorded by a user.</li>
 *   <li>{@code NOT_SENT -> REFUSED}: refusal recorded by a user (in person, phone).</li>
 *   <li>{@code SENT -> GIVEN}: person confirms via the e-mail link, or a user records it.</li>
 *   <li>{@code SENT -> REFUSED}: person refuses via the e-mail link, or a user records it.</li>
 *   <li>{@code GIVEN -> WITHDRAWN}: person withdraws consent (data-subject request or user record).</li>
 *   <li>{@code REFUSED -> SENT}: a new consent request is sent.</li>
 *   <li>{@code REFUSED -> GIVEN}: person changes their mind and consent is recorded.</li>
 *   <li>{@code WITHDRAWN -> SENT}: a new consent request is sent.</li>
 *   <li>{@code WITHDRAWN -> GIVEN}: person consents again and it is recorded.</li>
 * </ul>
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
            case SENT -> EnumSet.of(GIVEN, REFUSED);
            case GIVEN -> EnumSet.of(WITHDRAWN);
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
