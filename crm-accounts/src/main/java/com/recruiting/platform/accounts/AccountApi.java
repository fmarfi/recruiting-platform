package com.recruiting.platform.accounts;

import com.recruiting.platform.contracts.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Public read API of accounts for other modules (ADR 0001): references and existence checks. */
public interface AccountApi {

    Optional<AccountSummary> findById(UUID id);

    Map<UUID, AccountSummary> findByIds(Collection<UUID> ids);

    default boolean exists(UUID id) {
        return findById(id).isPresent();
    }

    /** Ids of accounts in this sector and status (either may be null = any); used to filter job order search. */
    java.util.Set<UUID> idsBySectorAndStatus(String sector, AccountStatus status);

    /** Number of accounts in the given status (My day statistics in pipeline). */
    long countByStatus(AccountStatus status);

    /** Archive and VAT facts of an account (agreements); empty when the account does not exist. */
    Optional<BillingFacts> billingFacts(UUID id);

    /** Contacts by id (missing ids are left out): which account, which person, role, and whether still active. */
    Map<UUID, ContactRef> findContacts(Collection<UUID> contactIds);

    /** Archive and VAT flags. */
    record BillingFacts(boolean archived, boolean vatExempt) {
    }

    /** A contact: a person in a role at an account; {@code active} = not ended. */
    record ContactRef(UUID id, UUID accountId, UUID personId, String role, boolean active) {
    }

    /** Fields beyond ownerId were added for pipeline's My day, which shows full account cards. */
    record AccountSummary(UUID id, String name, String sector, AccountStatus status, UUID ownerId, String city,
            String website, String notes, Instant statusChangedAt, LocalDate openedOn, int openJobCount,
            Instant lastActivityAt, Instant createdAt, Instant updatedAt, Long version) {
    }
}
