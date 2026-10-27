package com.recruiting.platform.pipeline.bulk.web.dto;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.ListEntity;
import java.util.List;
import java.util.UUID;

/** OpenAPI {@code BulkActionResult}. */
public record BulkActionResponse(UUID id, ListEntity entity, BulkActionType action, int requested, int ok,
        int skipped, int failed, List<BulkRecordResponse> results) {
}
