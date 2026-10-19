package com.recruiting.platform.security.audit.app;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.domain.AccessLog;
import com.recruiting.platform.security.domain.AccessLogRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the append-only access log (NFR-05), newest first. */
@Service
public class AccessLogQueryService {

    private final AccessLogRepository log;

    AccessLogQueryService(AccessLogRepository log) {
        this.log = log;
    }

    @Transactional(readOnly = true)
    public Page<AccessLog> find(UUID userId, EntityType entityType, UUID entityId, AccessAction action,
            Instant from, Instant to, int page, int size) {
        Specification<AccessLog> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (userId != null) {
                parts.add(cb.equal(root.get("userId"), userId));
            }
            if (entityType != null) {
                parts.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                parts.add(cb.equal(root.get("entityId"), entityId));
            }
            if (action != null) {
                parts.add(cb.equal(root.get("action"), action));
            }
            if (from != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("at"), from));
            }
            if (to != null) {
                parts.add(cb.lessThanOrEqualTo(root.get("at"), to));
            }
            return cb.and(parts.toArray(new Predicate[0]));
        };
        return log.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "at")));
    }
}
