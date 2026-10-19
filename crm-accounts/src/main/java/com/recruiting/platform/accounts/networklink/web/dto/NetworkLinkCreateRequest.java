package com.recruiting.platform.accounts.networklink.web.dto;

import com.recruiting.platform.contracts.LinkType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Body of POST /network-links. */
public record NetworkLinkCreateRequest(@NotNull UUID fromPersonId, @NotNull UUID toPersonId,
        @NotNull LinkType type, @Size(max = 1000) String note) {
}
