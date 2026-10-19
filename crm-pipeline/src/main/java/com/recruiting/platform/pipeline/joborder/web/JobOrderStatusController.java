package com.recruiting.platform.pipeline.joborder.web;

import com.recruiting.platform.pipeline.joborder.app.JobOrderMapper;
import com.recruiting.platform.pipeline.joborder.app.JobOrderStatusService;
import com.recruiting.platform.pipeline.joborder.web.dto.JobOrderResponse;
import com.recruiting.platform.pipeline.joborder.web.dto.JobOrderStatusChangeRequest;
import com.recruiting.platform.pipeline.shared.Actor;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PATCH /job-orders/{id}/status. Manager-only moves are decided in the service from the actor. */
@RestController
@RequestMapping("/api/v1/job-orders/{id}/status")
@PreAuthorize("hasRole('CONSULTANT')")
public class JobOrderStatusController {

    private final JobOrderStatusService service;
    private final JobOrderMapper mapper;

    JobOrderStatusController(JobOrderStatusService service, JobOrderMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PatchMapping
    JobOrderResponse change(@PathVariable UUID id, @Valid @RequestBody JobOrderStatusChangeRequest r,
            Authentication auth) {
        return mapper.toResponse(service.change(id, r.status(), r.reason(), r.version(), Actor.of(auth)));
    }
}
