package com.recruiting.platform.pipeline;

import java.util.UUID;

/**
 * Pipeline's step of a person merge (ADR 0011 decision 6). privacy orchestrates the merge in one transaction and calls
 * this step; it joins that transaction and never commits on its own.
 */
public interface PipelinePersonApi {

    /**
     * Moves applications, activities, next actions, exposures and timeline entries of the merged person to the
     * survivor. When both are on the same job order the survivor's application is kept and the merged one is deleted
     * (its exposures then point at the survivor's application).
     *
     * @param survivorId the person that remains
     * @param mergedId   the person that is merged away and deleted afterwards
     * @return how many rows of each kind moved
     */
    MergeResult mergePerson(UUID survivorId, UUID mergedId);

    /**
     * Rows moved by {@link #mergePerson}.
     *
     * @param applications       applications moved to the survivor
     * @param droppedApplications duplicates on the same job order that were deleted
     * @param activities         activities moved
     * @param nextActions        next actions moved
     * @param exposures          exposures moved
     * @param timelineEntries    timeline entries moved
     */
    record MergeResult(int applications, int droppedApplications, int activities, int nextActions, int exposures,
            int timelineEntries) {
    }
}
