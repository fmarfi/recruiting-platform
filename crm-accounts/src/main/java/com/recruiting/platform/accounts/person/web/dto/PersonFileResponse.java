package com.recruiting.platform.accounts.person.web.dto;

import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.TextExtractionStatus;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.UUID;

/** The PersonFile schema (1.2.0). */
public record PersonFileResponse(UUID id, UUID personId, PersonFileKind kind, int version, boolean current,
        String fileName, String contentType, long sizeBytes, TextExtractionStatus textStatus, Instant uploadedAt,
        UserRef uploadedBy) {
}
