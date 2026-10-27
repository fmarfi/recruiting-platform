package com.recruiting.platform.privacy.merge.web;

import com.recruiting.platform.privacy.merge.app.PersonMergeOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /people/{id}/merge}: merge a duplicate into this person (MANAGER/ADMIN, not reversible). */
@RestController
@RequestMapping("/api/v1/people/{id}/merge")
@PreAuthorize("hasRole('MANAGER')")
class MergeController {

    /** Body of the merge request. */
    record MergeRequest(@NotNull UUID mergedPersonId, List<String> takeFromMerged, @NotNull Long version) {
    }

    private final PersonMergeOrchestrator service;

    MergeController(PersonMergeOrchestrator service) {
        this.service = service;
    }

    @PostMapping
    Object merge(@PathVariable UUID id, @Valid @RequestBody MergeRequest body) {
        return service.merge(id, body.mergedPersonId(), body.takeFromMerged(), body.version());
    }
}
