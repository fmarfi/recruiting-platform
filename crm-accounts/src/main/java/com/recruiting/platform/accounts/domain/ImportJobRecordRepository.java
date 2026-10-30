package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ImportJobRecord}. */
public interface ImportJobRecordRepository extends JpaRepository<ImportJobRecord, ImportJobRecord.Key> {
    List<ImportJobRecord> findByIdImportId(UUID importId);
}
