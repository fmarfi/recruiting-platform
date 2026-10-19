package com.recruiting.platform.pipeline.shared.ref;

import com.recruiting.platform.accounts.AccountApi;
import com.recruiting.platform.contracts.AccountStatus;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** {@link AccountDirectory} over the accounts module's public {@link AccountApi}. */
@Component
class AccountDirectoryAdapter implements AccountDirectory {

    private final AccountApi api;

    AccountDirectoryAdapter(AccountApi api) {
        this.api = api;
    }

    @Override
    public Map<UUID, AccountInfo> accounts(Collection<UUID> ids) {
        return api.findByIds(ids).values().stream().map(s -> new AccountInfo(s.id(), s.name(), s.sector(), s.city(),
                s.website(), s.notes(), s.ownerId(), s.status(), s.statusChangedAt(), s.openedOn(), s.openJobCount(),
                s.lastActivityAt(), s.createdAt(), s.updatedAt(), s.version()))
                .collect(Collectors.toMap(AccountInfo::id, a -> a));
    }

    @Override
    public long countActiveAccounts() {
        return api.countByStatus(AccountStatus.ACTIVE);
    }
}
