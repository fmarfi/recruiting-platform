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

    @Override
    public java.util.Set<UUID> idsBySectorAndStatus(String sector, com.recruiting.platform.contracts.AccountStatus status) {
        return accounts.findAll((root, q, cb) -> {
            var all = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (sector != null) {
                all.add(cb.equal(cb.lower(root.get("sector")), sector.toLowerCase()));
            }
            if (status != null) {
                all.add(cb.equal(root.get("status"), status));
            }
            return cb.and(all.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }).stream().map(Account::getId).collect(Collectors.toSet());
    }

    @Override
    public long countByStatus(com.recruiting.platform.contracts.AccountStatus status) {
        return accounts.count((root, q, cb) -> cb.equal(root.get("status"), status));
    }

    private static AccountSummary toSummary(Account a) {
        return new AccountSummary(a.getId(), a.getName(), a.getSector(), a.getStatus(), a.getOwnerId(), a.getCity(),
                a.getWebsite(), a.getNotes(), a.getStatusChangedAt(), a.getOpenedOn(),
                a.getOpenJobCount() == null ? 0 : a.getOpenJobCount(), a.getLastActivityAt(), a.getCreatedAt(),
                a.getUpdatedAt(), a.getVersion());
    }
}
