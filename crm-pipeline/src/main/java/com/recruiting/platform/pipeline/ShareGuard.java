package com.recruiting.platform.pipeline;

import com.recruiting.platform.contracts.ShareBlockReason;
import java.util.UUID;

/**
 * Sharing guard SPI (ADR 0011 decision 8, Product Plan module 9). pipeline asks it on every move of an application
 * into a client-facing stage ({@code PipelineStage.sharedWithClient()}); privacy implements it from its master data
 * (a person is shareable only with consent GIVEN and not anonymised), so pipeline needs no dependency on privacy.
 * Wave 2d reports and share links call the same SPI. The answer is always fresh, never from a cached copy.
 */
public interface ShareGuard {

    /**
     * Tells whether the person may be shown to a client.
     *
     * @param personId the person
     * @return the decision, with the reason when blocked
     */
    ShareDecision check(UUID personId);

    /**
     * Decision of the guard.
     *
     * @param shareable {@code true} when the person may be shared
     * @param reason    why not; {@code null} when shareable
     */
    record ShareDecision(boolean shareable, ShareBlockReason reason) {

        /** @return a positive decision */
        public static ShareDecision allowed() {
            return new ShareDecision(true, null);
        }

        /**
         * @param reason why the person cannot be shared
         * @return a negative decision
         */
        public static ShareDecision blocked(ShareBlockReason reason) {
            return new ShareDecision(false, reason);
        }
    }
}
