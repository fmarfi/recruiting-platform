package com.recruiting.platform.pipeline.activity.app;

import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.pipeline.domain.Activity;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Filters of GET /activities. */
public record ActivityFilter(UUID accountId, UUID personId, UUID jobOrderId, List<ActivityType> types, UUID userId,
                             Instant from, Instant to) {

    Specification<Activity> toSpec() {
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (accountId != null) {
                ps.add(cb.equal(root.get("accountId"), accountId));
            }
            if (personId != null) {
                ps.add(cb.equal(root.get("personId"), personId));
            }
            if (jobOrderId != null) {
                ps.add(cb.equal(root.get("jobOrderId"), jobOrderId));
            }
            if (types != null && !types.isEmpty()) {
                ps.add(root.get("type").in(types));
            }
            if (userId != null) {
                ps.add(cb.equal(root.get("userId"), userId));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), from));
            }
            if (to != null) {
                ps.add(cb.lessThanOrEqualTo(root.<Instant>get("occurredAt"), to));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
