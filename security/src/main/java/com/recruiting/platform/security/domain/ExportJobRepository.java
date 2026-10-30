package com.recruiting.platform.security.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ExportJob}. */
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {
    List<ExportJob> findByStatusOrderByRequestedAtAsc(com.recruiting.platform.contracts.ExportStatus status);
}
