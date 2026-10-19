package com.recruiting.platform.accounts.networklink.web.dto;

import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.dto.PersonRef;
import com.recruiting.platform.contracts.LinkType;

/** One (contact, linked person) pair of an account (A-13 company view). */
public record AccountConnectionResponse(PersonRef contact, PersonSummaryResponse person, LinkType linkType,
        String direction) {
}
