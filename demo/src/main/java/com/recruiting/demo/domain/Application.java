package com.recruiting.demo.domain;

import java.time.LocalDate;

/**
 * Ties one candidate to one job order and records their pipeline stage
 * (requirement A-07). The same person can have applications in several job orders.
 *
 * @param id              unique id, e.g. {@code app-1}
 * @param jobOrderId      the job order
 * @param personId        the candidate
 * @param stage           current pipeline stage
 * @param rejectionReason reason when the stage is {@link PipelineStage#REJECTED}, else {@code null}
 * @param updated         date of the last stage change
 */
public record Application(String id, String jobOrderId, String personId, PipelineStage stage,
                          String rejectionReason, LocalDate updated) {

    /**
     * Returns a copy moved to a new stage.
     *
     * @param newStage the stage to set
     * @param reason   rejection reason, or {@code null}
     * @param date     date of the move
     * @return the updated application
     */
    public Application moveTo(PipelineStage newStage, String reason, LocalDate date) {
        return new Application(id, jobOrderId, personId, newStage, reason, date);
    }
}
