package com.recruiting.platform.privacy.dsr.web.dto;

import com.recruiting.platform.contracts.DataSubjectRequestStatus;
import com.recruiting.platform.contracts.DataSubjectRequestType;
import com.recruiting.platform.security.UserRef;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Shapes of the Data-subject requests tag. */
public final class DsrDtos {

    private DsrDtos() {
    }

    public record PersonRef(UUID id, String name) {
    }

    public record DsrResponse(UUID id, PersonRef person, String requesterName, String requesterEmail,
            DataSubjectRequestType type, DataSubjectRequestStatus status, String description, Instant receivedAt,
            LocalDate dueOn, boolean overdue, Instant resolvedAt, String resolution, UserRef handledBy,
            Instant createdAt, UserRef createdBy, long version) {
    }

    public record DsrCreate(UUID personId, @NotBlank @Size(max = 200) String requesterName,
            @NotBlank @Email @Size(max = 254) String requesterEmail, @NotNull DataSubjectRequestType type,
            @Size(max = 4000) String description, Instant receivedAt) {
    }

    public record DsrUpdate(UUID personId, DataSubjectRequestStatus status, String reason,
            @Size(max = 4000) String resolution, UUID handledById, Long version) {
    }

    public record AnonymiseBody(@NotBlank @Size(max = 1000) String reason, UUID dataSubjectRequestId) {
    }
}
