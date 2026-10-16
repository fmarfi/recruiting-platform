package com.recruiting.platform.privacy.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionRunRepository extends JpaRepository<RetentionRun, UUID> {
}
