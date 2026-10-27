package com.recruiting.platform.accounts.duplicate.web.dto;

import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.contracts.DuplicateMatchReason;
import java.util.List;

/** The DuplicateMatch schema. */
public record DuplicateMatchResponse(PersonSummaryResponse person, List<DuplicateMatchReason> reasons) {
}
