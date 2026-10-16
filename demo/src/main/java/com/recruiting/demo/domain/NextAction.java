package com.recruiting.demo.domain;

import java.time.LocalDate;

/**
 * A follow-up with an owner and a due date, shown on "My day" (requirement A-10).
 *
 * @param id        unique id
 * @param title     what to do
 * @param due       due date
 * @param owner     who must do it
 * @param accountId related account, or {@code null}
 * @param personId  related person, or {@code null}
 * @param done      whether it is completed
 */
public record NextAction(String id, String title, LocalDate due, String owner,
                         String accountId, String personId, boolean done) {

    /**
     * Returns a completed copy.
     *
     * @return the action marked as done
     */
    public NextAction complete() {
        return new NextAction(id, title, due, owner, accountId, personId, true);
    }
}
