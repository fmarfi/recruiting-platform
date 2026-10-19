package com.recruiting.platform.pipeline.timeline.web;

import com.recruiting.platform.contracts.TimelineKind;
import com.recruiting.platform.pipeline.shared.dto.PageResponse;
import com.recruiting.platform.pipeline.timeline.app.TimelineService;
import com.recruiting.platform.pipeline.timeline.web.dto.TimelineEntryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /accounts/{id}/timeline (openapi tag Account overview). */
@RestController
@RequestMapping("/api/v1/accounts/{id}/timeline")
@PreAuthorize("hasRole('CONSULTANT')")
public class TimelineController {

    private final TimelineService service;

    TimelineController(TimelineService service) {
        this.service = service;
    }

    @GetMapping
    PageResponse<TimelineEntryResponse> timeline(@PathVariable UUID id,
            @RequestParam(required = false) List<TimelineKind> kind, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(service.page(id, kind, Math.max(0, page), Math.min(100, Math.max(1, size))),
                service::toResponses);
    }
}
