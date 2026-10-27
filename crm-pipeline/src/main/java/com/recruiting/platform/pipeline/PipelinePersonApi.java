package com.recruiting.platform.pipeline;

import java.util.UUID;

/**
 * Merge step in pipeline (openapi {@code POST /people/{id}/merge}, step 2), called by privacy inside the merge
 * transaction.
 *
 * <p>NOTE: minimal copy written by security-privacy so that privacy compiles; backend-pipeline's definition wins at
 * merge if it differs.
 */
public interface PipelinePersonApi {

    /**
     * Moves applications (same job order: keep the further stage and drop the other), activities, next actions,
     * exposures and timeline entries from one person to the other.
     */
    void movePerson(UUID fromPersonId, UUID toPersonId);
}
