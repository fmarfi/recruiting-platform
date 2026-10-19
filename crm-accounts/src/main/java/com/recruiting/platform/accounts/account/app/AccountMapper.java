package com.recruiting.platform.accounts.account.app;

import com.recruiting.platform.accounts.account.web.dto.AccountResponse;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Account entity to response record; owners are looked up once per list. */
@Component
public class AccountMapper {

    private final UserDirectory users;

    public AccountMapper(UserDirectory users) {
        this.users = users;
    }

    public List<AccountResponse> responses(Collection<Account> accounts) {
        Map<UUID, UserRef> owners = users.refs(accounts.stream().map(Account::getOwnerId).toList());
        return accounts.stream().map(a -> response(a, owners)).toList();
    }

    public AccountResponse response(Account a) {
        return response(a, users.refs(List.of(a.getOwnerId())));
    }

    private AccountResponse response(Account a, Map<UUID, UserRef> owners) {
        return new AccountResponse(a.getId(), a.getName(), a.getSector(), a.getCity(), a.getWebsite(), a.getNotes(),
                owners.get(a.getOwnerId()), a.getStatus(), a.getStatusChangedAt(), a.getOpenedOn(),
                a.getOpenJobCount() == null ? 0 : a.getOpenJobCount(), a.getLastActivityAt(), a.getCreatedAt(),
                a.getUpdatedAt(), a.getVersion() == null ? 0 : a.getVersion());
    }
}
