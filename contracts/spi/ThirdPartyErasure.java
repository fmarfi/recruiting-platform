package com.recruiting.platform.contracts;

import java.util.List;
import java.util.UUID;

/**
 * Erases or lists data a module holds about a third person who is identified only by e-mail
 * (since contract 1.3.0, review round; {@code contracts/personal-data.md}). {@code privacy} calls
 * every bean for a data-subject request: ACCESS lists the matches, a completed DELETION erases
 * them, whether or not the requester is a {@code person}. {@code accounts} implements it for
 * {@code account.billing_emails}, {@code pipeline} for the referee fields of
 * {@code candidate_reference}. Each implementation also purges the matching Envers history.
 */
public interface ThirdPartyErasure {

    /**
     * Finds records holding the e-mail (case-insensitive, trimmed).
     *
     * @param email the requester's e-mail
     * @return ids of the matching records, for the ACCESS export
     */
    List<UUID> findByEmail(String email);

    /**
     * Removes the e-mail and the data tied to it; idempotent.
     *
     * @param email the requester's e-mail
     * @return number of records changed
     */
    int eraseEmail(String email);

    /**
     * Removes a sole trader's TCKN used as an organisation's tax id ({@code account.tax_id} and its
     * history); called when the DSR handler enters the requester's TCKN. Only {@code accounts}
     * holds tax ids, so the default does nothing.
     *
     * @param taxId the 11-digit TCKN as entered
     * @return number of records changed
     */
    default int eraseTaxId(String taxId) {
        return 0;
    }
}
