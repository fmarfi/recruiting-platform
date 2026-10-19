package com.recruiting.platform.security.audit.web;

import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.ApiPage;
import com.recruiting.platform.security.audit.app.RevisionQueryService;
import com.recruiting.platform.security.audit.web.dto.RevisionResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tag Audit: change history of one record (managers and admins). */
@RestController
@RequestMapping("/api/v1/audit/revisions")
class RevisionController {

    private final RevisionQueryService revisions;

    RevisionController(RevisionQueryService revisions) {
        this.revisions = revisions;
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    ApiPage<RevisionResponse> list(@RequestParam EntityType entityType, @RequestParam UUID entityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiPage.of(revisions.history(entityType, entityId, page, Math.min(Math.max(size, 1), 100)), r -> r);
    }
}
