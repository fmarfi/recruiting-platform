package com.recruiting.platform.accounts.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    List<ImportJob> findByCreatedAtBeforeAndFileContentIsNotNull(Instant before);
}
