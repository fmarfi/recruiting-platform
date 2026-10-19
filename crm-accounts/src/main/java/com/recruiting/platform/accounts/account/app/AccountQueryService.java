package com.recruiting.platform.accounts.account.app;

import com.recruiting.platform.accounts.account.web.dto.AccountResponse;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.shared.Specs;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.AccountStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Filtered accounts overview (A-01) and the account predicate reused by global search (A-14). */
@Service
@Transactional(readOnly = true)
public class AccountQueryService {

    private final AccountRepository accounts;
    private final AccountMapper mapper;

    public AccountQueryService(AccountRepository accounts, AccountMapper mapper) {
        this.accounts = accounts;
        this.mapper = mapper;
    }

    public PageResponse<AccountResponse> list(String q, List<AccountStatus> statuses, String sector, UUID ownerId,
            Pageable pageable) {
        Page<Account> page = accounts.findAll(spec(q, statuses, sector, ownerId), pageable);
        return PageResponse.of(page, mapper.responses(page.getContent()));
    }

    public Specification<Account> spec(String q, List<AccountStatus> statuses, String sector, UUID ownerId) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                all.add(Specs.unaccentContains(cb, root.get("name"), q));
            }
            if (statuses != null && !statuses.isEmpty()) {
                all.add(root.get("status").in(statuses));
            }
            if (sector != null && !sector.isBlank()) {
                all.add(cb.equal(root.get("sector"), sector));
            }
            if (ownerId != null) {
                all.add(cb.equal(root.get("ownerId"), ownerId));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }
}
