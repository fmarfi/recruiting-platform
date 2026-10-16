package com.recruiting.demo.domain;

import java.time.LocalDate;

/**
 * A commercial proposal to a client account (requirement A-05).
 *
 * @param id        unique id, e.g. {@code pro-1}
 * @param accountId the client account
 * @param title     what the proposal covers
 * @param terms     commercial terms, free text
 * @param status    current proposal status
 * @param date      date of the last change
 */
public record Proposal(String id, String accountId, String title, String terms,
                       ProposalStatus status, LocalDate date) {

    /**
     * Returns a copy with a new status.
     *
     * @param newStatus the status to set
     * @param on        date of the change
     * @return the updated proposal
     */
    public Proposal withStatus(ProposalStatus newStatus, LocalDate on) {
        return new Proposal(id, accountId, title, terms, newStatus, on);
    }
}
