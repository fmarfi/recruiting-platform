package com.recruiting.platform.pipeline.nextaction.web.dto;

import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.shared.dto.PersonRef;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** OpenAPI NextAction. */
public record NextActionResponse(UUID id, String title, LocalDate dueOn, UserRef owner, AccountRef account,
                                 PersonRef person, JobOrderRef jobOrder, boolean done, Instant doneAt,
                                 boolean overdue, Instant createdAt, Long version) {
}
