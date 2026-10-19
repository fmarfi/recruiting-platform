package com.recruiting.platform.security.audit.web.dto;

import com.recruiting.platform.contracts.RevisionType;
import com.recruiting.platform.security.UserRef;
import java.time.Instant;
import java.util.List;

/** Contract schemas Revision and FieldChange. */
public record RevisionResponse(long revision, Instant at, UserRef user, RevisionType type,
        List<FieldChange> changes) {

    /** One changed field; {@code from} is null for new records, {@code to} for cleared values. */
    public record FieldChange(String field, Object from, Object to) {
    }
}
