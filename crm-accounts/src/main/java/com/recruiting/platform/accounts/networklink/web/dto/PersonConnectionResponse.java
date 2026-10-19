package com.recruiting.platform.accounts.networklink.web.dto;

import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.contracts.LinkType;
import java.util.UUID;

/** One connection of a person (A-13). */
public record PersonConnectionResponse(UUID linkId, LinkType type, String direction, PersonSummaryResponse person, String note) {
}
