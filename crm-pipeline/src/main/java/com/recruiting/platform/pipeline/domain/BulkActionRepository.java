package com.recruiting.platform.pipeline.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkActionRepository extends JpaRepository<BulkAction, UUID> {
}
