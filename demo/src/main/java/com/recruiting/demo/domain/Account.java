package com.recruiting.demo.domain;

import java.time.LocalDate;

/**
 * A client company; one account per client (requirement A-01).
 *
 * @param id     unique id, e.g. {@code acc-1}
 * @param name   company name
 * @param sector business sector used for filtering
 * @param city   main city of the client
 * @param owner  responsible consultant
 * @param status current account status (A-02)
 * @param since  date the account was opened
 */
public record Account(String id, String name, String sector, String city, String owner,
                      AccountStatus status, LocalDate since) {

    /**
     * Returns a copy with a new status.
     *
     * @param newStatus the status to set
     * @return the updated account
     */
    public Account withStatus(AccountStatus newStatus) {
        return new Account(id, name, sector, city, owner, newStatus, since);
    }
}
