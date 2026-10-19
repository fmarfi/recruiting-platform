package com.recruiting.platform.accounts.importing.app;

import com.recruiting.platform.accounts.account.web.dto.AccountCreateRequest;
import com.recruiting.platform.accounts.importing.web.dto.ImportRowError;
import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import java.util.List;
import java.util.UUID;

/**
 * Result of validating a file: counts, the first 500 row errors and the rows that can be written.
 *
 * @param errorRows number of rows with at least one error (errors list is capped, this is not)
 */
public record ImportAnalysis(int totalRows, int errorRows, int duplicateRows, List<ImportRowError> errors,
        List<Row> valid) {

    public static final int MAX_ERRORS = 500;

    /** One writable row: an account, or a person (with the account and role when it is a contact). */
    public record Row(AccountCreateRequest account, PersonCreateRequest person, UUID accountId, String role) {
    }
}
