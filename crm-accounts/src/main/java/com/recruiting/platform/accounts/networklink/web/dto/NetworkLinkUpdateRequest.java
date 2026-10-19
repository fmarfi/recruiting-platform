package com.recruiting.platform.accounts.networklink.web.dto;

import com.recruiting.platform.contracts.LinkType;
import jakarta.validation.constraints.Size;

/** PATCH body for a link. */
public record NetworkLinkUpdateRequest(LinkType type, @Size(max = 1000) String note) {
}
