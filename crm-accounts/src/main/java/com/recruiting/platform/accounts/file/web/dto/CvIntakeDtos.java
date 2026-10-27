package com.recruiting.platform.accounts.file.web.dto;

import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateMatchResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import com.recruiting.platform.accounts.person.web.dto.PersonFileResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonResponse;
import com.recruiting.platform.accounts.shared.dto.PersonRef;
import com.recruiting.platform.contracts.CvIntakeStatus;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.contracts.TextExtractionStatus;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response records of /cv-intake. */
public final class CvIntakeDtos {

    private CvIntakeDtos() {
    }

    public record Detected(List<String> emails, List<String> phones, List<String> linkedinUrls) {
    }

    public record Item(UUID id, UUID batchId, CvIntakeStatus status, String fileName, String contentType,
            Long sizeBytes, TextExtractionStatus textStatus, Detected detected, List<DuplicateMatchResponse> duplicates,
            SourceChannel sourceChannel, Instant uploadedAt, UserRef uploadedBy, Instant expiresAt,
            PersonRef attachedPerson) {
    }

    public record Rejected(String fileName, String code) {
    }

    public record Batch(UUID batchId, List<Item> items, List<Rejected> rejected) {
    }

    /** Exactly one of the two members. */
    public record AttachRequest(UUID personId, @jakarta.validation.Valid PersonCreateRequest person) {
    }

    public record AttachResult(PersonResponse person, PersonFileResponse file) {
    }
}
