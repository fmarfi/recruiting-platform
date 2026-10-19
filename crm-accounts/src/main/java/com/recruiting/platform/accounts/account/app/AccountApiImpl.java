package com.recruiting.platform.accounts.account.app;

import com.recruiting.platform.accounts.AccountApi;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements the public AccountApi for other modules; entities never leave accounts. */
@Service
@Transactional(readOnly = true)
public class AccountApiImpl implements AccountApi {

    private final AccountRepository accounts;

    public AccountApiImpl(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<AccountSummary> findById(UUID id) {
        return accounts.findById(id).map(AccountApiImpl::toSummary);
    }

    @Override
    public Map<UUID, AccountSummary> findByIds(Collection<UUID> ids) {
        return accounts.findAllByIdIn(ids).stream().map(AccountApiImpl::toSummary)
                .collect(Collectors.toMap(AccountSummary::id, Function.identity()));
    }

    private static AccountSummary toSummary(Account a) {
        return new AccountSummary(a.getId(), a.getName(), a.getSector(), a.getStatus(), a.getOwnerId());
    }
}
