package com.recruiting.platform.accounts.importing.web.dto;

import com.recruiting.platform.accounts.shared.dto.UserRef;
import com.recruiting.platform.contracts.ImportKind;
import com.recruiting.platform.contracts.ImportStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Import job as in the ImportJob schema. */
public record ImportJobResponse(UUID id, ImportKind kind, ImportStatus status, String fileName, int totalRows,
        int validRows, int duplicateRows, int importedRows, List<ImportRowError> errors, String failureReason,
        Instant createdAt, UserRef createdBy, Instant completedAt) {
}
