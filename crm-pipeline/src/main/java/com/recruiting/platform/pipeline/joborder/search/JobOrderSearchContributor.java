package com.recruiting.platform.pipeline.joborder.search;

import com.recruiting.platform.accounts.AccountApi;
import com.recruiting.platform.accounts.SearchContributor;
import com.recruiting.platform.contracts.SearchType;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.domain.JobOrderRepository;
import com.recruiting.platform.pipeline.joborder.app.JobOrderMapper;
import com.recruiting.platform.pipeline.shared.Specs;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plugs job orders into the accounts module's global search (A-14, ADR 0001). Matches on title; the sector and
 * account-status filters are resolved through the accounts API into account ids.
 */
@Component
class JobOrderSearchContributor implements SearchContributor {

    private final JobOrderRepository repo;
    private final JobOrderMapper mapper;
    private final AccountApi accounts;

    JobOrderSearchContributor(JobOrderRepository repo, JobOrderMapper mapper, AccountApi accounts) {
        this.repo = repo;
        this.mapper = mapper;
        this.accounts = accounts;
    }

    @Override
    public SearchType type() {
        return SearchType.JOB_ORDER;
    }

    @Override
    @Transactional(readOnly = true)
    public Result search(Criteria c) {
        Set<UUID> accountIds = c.sector() == null && c.accountStatus() == null ? null
                : accounts.idsBySectorAndStatus(c.sector(), c.accountStatus());
        if (accountIds != null && accountIds.isEmpty()) {
            return new Result(List.of(), 0);
        }
        Specification<JobOrder> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(Specs.containsUnaccented(cb, root.get("title"), c.query()));
            if (accountIds != null) {
                ps.add(root.get("accountId").in(accountIds));
            }
            if (c.jobOrderStatus() != null) {
                ps.add(cb.equal(root.get("status"), c.jobOrderStatus()));
            }
            if (c.ownerId() != null) {
                ps.add(cb.equal(root.get("ownerId"), c.ownerId()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<JobOrder> found = repo.findAll(spec, PageRequest.of(0, c.limit(), Sort.by("title")));
        return new Result(mapper.toResponses(found.getContent()), found.getTotalElements());
    }
}
