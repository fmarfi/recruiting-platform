package com.recruiting.platform.pipeline;

import com.recruiting.platform.contracts.ShareBlockReason;
import java.util.Optional;
import java.util.UUID;

/**
 * Sharing guard SPI (ADR 0011 decision 8). Defined here, in the pipeline public package, so that pipeline can block a
 * move into a client-facing stage with 409 {@code CONSENT_REQUIRED} without depending on {@code privacy}; privacy
 * implements it from its master data (consent status in {@code person_privacy}), so the answer is never stale.
 * Wave 2d reports and share links call the same SPI.
 *
 * <p>NOTE: if backend-pipeline's branch defines this interface differently, theirs wins and the privacy
 * implementation ({@code ShareGuardImpl}) is adapted at merge; this copy only exists so that privacy compiles.
 */
public interface ShareGuard {

    /**
     * @param personId the person to share
     * @return empty when the person does not exist; otherwise the decision
     */
    Optional<ShareCheck> check(UUID personId);

    /**
     * Decision for one person.
     *
     * @param personId  the person
     * @param shareable consent GIVEN and not anonymised
     * @param reason    null when shareable
     */
    record ShareCheck(UUID personId, boolean shareable, ShareBlockReason reason) {
    }
}
