package com.recruiting.platform.accounts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public API of accounts for privacy (ADR 0011, schema.md "Pending intake items and DSRs"): pending CV intake items
 * hold personal data of people who may not be a {@code person} yet, so a data-subject request reaches them by e-mail.
 */
public interface CvIntakeApi {

    /** PENDING items whose detected e-mail addresses contain {@code email} (compared lower-cased). */
    List<PendingIntake> findPendingByEmail(String email);

    /** Discards those items (bytes, text and detected values removed). Returns how many were discarded. */
    int discardPendingByEmail(String email);

    /** What a DSR export lists: file name, upload date and the detected values. */
    record PendingIntake(UUID id, String fileName, Instant uploadedAt, List<String> emails, List<String> phones,
            List<String> linkedinUrls) {
    }
}
