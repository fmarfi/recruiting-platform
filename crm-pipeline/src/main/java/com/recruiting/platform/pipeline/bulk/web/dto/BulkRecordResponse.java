package com.recruiting.platform.pipeline.bulk.web.dto;

import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import java.util.UUID;

/** OpenAPI {@code BulkRecordResult}: code and message are null when OK. */
public record BulkRecordResponse(UUID id, BulkOutcome outcome, BulkResultCode code, String message) {
}
