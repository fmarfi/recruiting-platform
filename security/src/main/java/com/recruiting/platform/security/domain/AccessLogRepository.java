package com.recruiting.platform.security.domain;

import com.recruiting.platform.contracts.EntityType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccessLogRepository extends JpaRepository<AccessLog, UUID>, JpaSpecificationExecutor<AccessLog> {

    Page<AccessLog> findByEntityTypeAndEntityIdOrderByAtDesc(EntityType entityType, UUID entityId, Pageable pageable);

    Page<AccessLog> findByUserIdOrderByAtDesc(UUID userId, Pageable pageable);
}
