package com.recruiting.platform.pipeline.bulk.web;

import com.recruiting.platform.pipeline.bulk.app.BulkActionService;
import com.recruiting.platform.pipeline.bulk.web.dto.BulkActionResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** POST /bulk-actions: one action on up to 500 selected records, a result per record (ADR 0010). */
@RestController
@RequestMapping("/api/v1/bulk-actions")
@PreAuthorize("hasRole('CONSULTANT')")
public class BulkActionController {

    private final BulkActionService service;

    BulkActionController(BulkActionService service) {
        this.service = service;
    }

    @PostMapping
    BulkActionResponse run(@RequestBody JsonNode body) {
        return service.run(body);
    }
}
