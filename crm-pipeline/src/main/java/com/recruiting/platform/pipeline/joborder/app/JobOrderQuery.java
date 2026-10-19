package com.recruiting.platform.pipeline.joborder.app;

import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.shared.Specs;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Filters of GET /job-orders. */
final class JobOrderQuery {

    private JobOrderQuery() {
    }

    static Specification<JobOrder> of(String q, UUID accountId, List<JobOrderStatus> statuses, Boolean open,
            UUID ownerId) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                ps.add(Specs.containsUnaccented(cb, root.get("title"), q));
            }
            if (accountId != null) {
                ps.add(cb.equal(root.get("accountId"), accountId));
            }
            if (statuses != null && !statuses.isEmpty()) {
                ps.add(root.get("status").in(statuses));
            }
            if (open != null) {
                Set<JobOrderStatus> openSet = EnumSet.allOf(JobOrderStatus.class);
                openSet.removeIf(s -> !s.isOpen());
                ps.add(open ? root.get("status").in(openSet) : cb.not(root.get("status").in(openSet)));
            }
            if (ownerId != null) {
                ps.add(cb.equal(root.get("ownerId"), ownerId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
