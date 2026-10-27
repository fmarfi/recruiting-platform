package com.recruiting.platform.accounts;

import com.recruiting.platform.contracts.OffLimitsReason;
import java.util.List;
import java.util.UUID;

/**
 * Off-limits check for pipeline (ADR 0011 decision 10). Minimal interface added by backend-pipeline so its guard
 * compiles; backend-accounts owns the implementation and may extend this file (the orchestrator reconciles).
 */
public interface OffLimitsApi {

    /**
     * Rules in force today that match the person's current company (person company, or an employment without end
     * date, normalised). Rules whose target is the given account are ignored.
     *
     * @param personId     the person
     * @param ownAccountId account of the job order; its own rules are skipped
     * @return the matching rules, empty when none
     */
    List<Match> match(UUID personId, UUID ownAccountId);

    /**
     * A matching rule.
     *
     * @param ruleId      rule id
     * @param accountId   target account, or {@code null} for a free company name
     * @param companyName name of the company (account name or free text, no personal data)
     * @param reason      why the company is off-limits
     */
    record Match(UUID ruleId, UUID accountId, String companyName, OffLimitsReason reason) {
    }
}
