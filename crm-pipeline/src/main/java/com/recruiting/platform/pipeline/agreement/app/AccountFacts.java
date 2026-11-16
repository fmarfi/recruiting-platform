package com.recruiting.platform.pipeline.agreement.app;

import com.recruiting.platform.pipeline.shared.ref.AccountDirectory;
import com.recruiting.platform.pipeline.shared.ref.AccountDirectory.ContactInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Account and contact facts that the agreement rules need, read through the accounts module (ADR 0001). */
@Component
class AccountFacts {

    /** Archive and VAT flags of an account. */
    record Flags(boolean archived, boolean vatExempt) {
    }

    private final AccountDirectory accounts;

    AccountFacts(AccountDirectory accounts) {
        this.accounts = accounts;
    }

    Flags flags(UUID accountId) {
        AccountDirectory.AccountFlags f = accounts.flags(accountId);
        return f == null ? null : new Flags(f.archived(), f.vatExempt());
    }

    boolean isActiveContactOf(UUID contactId, UUID accountId) {
        ContactInfo c = accounts.contacts(List.of(contactId)).get(contactId);
        return c != null && c.active() && c.accountId().equals(accountId);
    }

    /** Person id per contact id. */
    Map<UUID, UUID> personIds(Collection<UUID> contactIds) {
        Map<UUID, UUID> out = new HashMap<>();
        if (!contactIds.isEmpty()) {
            accounts.contacts(contactIds).values().forEach(c -> out.put(c.id(), c.personId()));
        }
        return out;
    }
}
