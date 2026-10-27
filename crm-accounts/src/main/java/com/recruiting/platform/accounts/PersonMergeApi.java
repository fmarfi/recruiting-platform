package com.recruiting.platform.accounts;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Public API of accounts for the merge orchestration in privacy (ADR 0011 item 6, schema.md). privacy runs, in one
 * transaction: {@link #mergeProfile} (step 1), the pipeline and privacy steps, then {@link #finish} (step 4). The
 * merged ids of a survivor are read with {@link #mergedIds} BEFORE any delete (purge step 0), because deleting a person
 * cascades to its {@code person_merge} rows.
 */
public interface PersonMergeApi {

    /** Field names accepted in {@code takeFromMerged} (PersonMergeRequest). */
    Set<String> TAKEABLE_FIELDS = Set.of("name", "type", "title", "company", "email", "phone", "city", "linkedinUrl",
            "howWeKnow", "notes", "ownerId", "sourceChannel", "noticePeriodDays", "mobility", "compensation");

    /**
     * Step 1: copies the named fields from the merged person (empty survivor fields are filled anyway), unions the
     * tags (at most 20), appends employments, educations and languages the survivor lacks, moves files (CVs
     * renumbered by upload time, identical bytes dropped), contacts and network links (duplicates and self-links
     * dropped) and closes duplicate pairs of both as MERGED.
     *
     * @param survivorVersion the survivor's {@code version} sent by the client (required)
     * @throws com.recruiting.platform.contracts.ApiException NOT_FOUND, STATE_CONFLICT (same id or anonymised),
     *                                                        VERSION_CONFLICT, VALIDATION_FAILED (unknown field)
     */
    void mergeProfile(UUID survivorId, UUID mergedId, Set<String> takeFromMerged, long survivorVersion, UUID actorId);

    /**
     * Step 4: writes {@code person_merge}, deletes the merged person through JPA (Envers DEL row) and publishes
     * {@code PersonMerged}. Call it after the pipeline and privacy steps moved their rows.
     */
    void finish(UUID survivorId, UUID mergedId, Set<String> fieldsTaken, UUID actorId);

    /** Purge step 0: every person id merged into {@code survivorId} so far. */
    List<UUID> mergedIds(UUID survivorId);
}
