package com.recruiting.platform.accounts.networklink.web.dto;

import com.recruiting.platform.accounts.shared.dto.PersonRef;
import com.recruiting.platform.security.UserRef;
import com.recruiting.platform.contracts.LinkType;
import java.time.Instant;
import java.util.UUID;

/** Link as in the NetworkLink schema. */
public record NetworkLinkResponse(UUID id, PersonRef from, PersonRef to, LinkType type, String note,
        Instant createdAt, UserRef createdBy) {
}
