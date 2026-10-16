package com.recruiting.platform.accounts.domain;

import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    /** Retention: drops the uploaded file and error list of imports older than the cutoff; returns rows changed. */
    @Modifying
    @Query("UPDATE ImportJob j SET j.fileContent = null, j.errors = null WHERE j.createdAt < :cutoff AND j.fileContent IS NOT NULL")
    int purgeFilesCreatedBefore(@Param("cutoff") Instant cutoff);
}
