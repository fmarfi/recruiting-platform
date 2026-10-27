package com.recruiting.platform.accounts;

import java.util.Set;
import java.util.UUID;

/**
 * Merge steps in accounts (ADR 0011 decision 6, openapi {@code POST /people/{id}/merge}); called by privacy, which
 * orchestrates the whole merge in one transaction. Both write methods join the caller's transaction.
 *
 * <p>NOTE: minimal copy written by security-privacy so that privacy compiles; backend-accounts' definition wins at
 * merge if it differs, and the call sites in {@code PersonMergeService} are adapted.
 */
public interface PersonMergeApi {

    /**
     * Step 1: copy the fields in {@code takeFromMerged}, fill empty survivor fields, union tags, append employments,
     * educations and languages, move files (CV versions renumbered), contacts and links, close duplicate pairs.
     *
     * @throws com.recruiting.platform.contracts.ApiException VERSION_CONFLICT when the survivor's version differs,
     *         STATE_CONFLICT when a person is anonymised, NOT_FOUND
     */
    void mergeProfile(UUID survivorId, UUID mergedId, Set<String> takeFromMerged, long survivorVersion, UUID actorId);

    /** Step 4: write {@code person_merge(survivor_id, merged_id)} and delete the merged person through JPA. */
    void completeMerge(UUID survivorId, UUID mergedId, Set<String> takeFromMerged, UUID actorId);

    /** The survivor as the API representation ({@code Person} schema), for the 200 response. */
    Object survivorView(UUID survivorId);
}
