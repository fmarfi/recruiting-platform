package com.recruiting.platform.accounts;

import com.recruiting.platform.contracts.OffLimitsReason;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public API of accounts for pipeline (ADR 0011 item 10): is the current company of a person off-limits? Ids and
 * dates only. A rule matches when its target (the account name or the free company name) equals the company,
 * accent- and case-insensitive; rules in force today only.
 */
public interface OffLimitsApi {

    /**
     * Active rules that match the person's current company (the {@code company} column, or the company of an open
     * employment). Rules of {@code ignoreAccountId} (the job order's own account) are left out. Empty for an unknown
     * or anonymised person.
     */
    List<Match> findForPerson(UUID personId, UUID ignoreAccountId);

    /** Active rules that match a company name; rules of {@code ignoreAccountId} are left out. */
    List<Match> findForCompany(String companyName, UUID ignoreAccountId);

    /** Convenience: true when {@link #findForPerson} is not empty. */
    default boolean isBlocked(UUID personId, UUID ignoreAccountId) {
        return !findForPerson(personId, ignoreAccountId).isEmpty();
    }

    record Match(UUID ruleId, UUID accountId, String companyName, OffLimitsReason reason, LocalDate startsOn,
            LocalDate endsOn) {
    }
}
