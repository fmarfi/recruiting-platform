package com.recruiting.platform.accounts.referencedata.web.dto;

import java.util.List;

/** A lifecycle: initial status and statuses in display order. */
public record LifecycleDefinition(String initial, List<StatusDefinition> statuses) {
}
