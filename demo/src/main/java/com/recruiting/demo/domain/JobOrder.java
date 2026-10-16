package com.recruiting.demo.domain;

import java.time.LocalDate;

/**
 * A role the firm is searching for inside a client account (requirement A-06).
 *
 * @param id        unique id, e.g. {@code job-1}
 * @param accountId the client account
 * @param title     role title
 * @param location  work location
 * @param seniority seniority level, e.g. "C-level", "Director"
 * @param status    current job status
 * @param opened    date the job order was opened
 * @param owner     lead consultant
 * @param fee       agreed fee terms, free text
 */
public record JobOrder(String id, String accountId, String title, String location,
                       String seniority, JobOrderStatus status, LocalDate opened,
                       String owner, String fee) {

    /**
     * Returns a copy with a new status.
     *
     * @param newStatus the status to set
     * @return the updated job order
     */
    public JobOrder withStatus(JobOrderStatus newStatus) {
        return new JobOrder(id, accountId, title, location, seniority, newStatus, opened,
                owner, fee);
    }
}
