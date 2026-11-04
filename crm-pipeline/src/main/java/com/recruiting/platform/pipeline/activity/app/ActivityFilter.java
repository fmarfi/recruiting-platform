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
                             Instant from, Instant to,
                             List<com.recruiting.platform.contracts.ActivityOutcome> outcomes,
                             List<com.recruiting.platform.contracts.ActivitySource> sources) {

    Specification<Activity> toSpec() {
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (accountId != null) {
                ps.add(cb.equal(root.get("accountId"), accountId));
            }
            if (personId != null) {
                var sub = q.subquery(UUID.class);
                var att = sub.from(com.recruiting.platform.pipeline.domain.ActivityAttendee.class);
                sub.select(att.get("id").<UUID>get("activityId")).where(
                        cb.equal(att.get("id").get("personId"), personId));
                ps.add(cb.or(cb.equal(root.get("personId"), personId), root.get("id").in(sub)));
            }
            if (jobOrderId != null) {
                ps.add(cb.equal(root.get("jobOrderId"), jobOrderId));
            }
            if (types != null && !types.isEmpty()) {
                ps.add(root.get("type").in(types));
            }
            if (outcomes != null && !outcomes.isEmpty()) {
                ps.add(root.get("outcome").in(outcomes));
            }
            if (sources != null && !sources.isEmpty()) {
                ps.add(root.get("source").in(sources));
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
