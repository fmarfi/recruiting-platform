package com.recruiting.platform.security;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.domain.AccessLog;
import com.recruiting.platform.security.domain.AccessLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the security module: write one append-only access-log row (who viewed, exported or downloaded
 * what, NFR-05). It joins the caller's transaction, so a failed request leaves no row.
 */
@Component
public class AccessRecorder {

    private final AccessLogRepository log;
    private final CurrentUser currentUser;

    AccessRecorder(AccessLogRepository log, CurrentUser currentUser) {
        this.log = log;
        this.currentUser = currentUser;
    }

    /** Records an access by the logged-in user; does nothing when nobody is logged in (system jobs). */
    @Transactional
    public void record(AccessAction action, EntityType entityType, UUID entityId, String ipAddress) {
        currentUser.idOrEmpty().ifPresent(userId -> write(userId, action, entityType, entityId, ipAddress));
    }

    public void record(AccessAction action, EntityType entityType, UUID entityId) {
        record(action, entityType, entityId, null);
    }

    /** Records an access for an explicit user (login happens before the session exists). */
    @Transactional
    public void recordFor(UUID userId, AccessAction action, EntityType entityType, UUID entityId, String ip) {
        write(userId, action, entityType, entityId, ip);
    }

    private void write(UUID userId, AccessAction action, EntityType type, UUID entityId, String ip) {
        AccessLog row = new AccessLog();
        row.setUserId(userId);
        row.setAction(action);
        row.setEntityType(type);
        row.setEntityId(entityId);
        row.setIpAddress(ip);
        log.save(row);
    }
}
