package com.recruiting.platform.pipeline.joborder.web;

import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.pipeline.joborder.app.JobOrderMapper;
import com.recruiting.platform.pipeline.joborder.app.JobOrderService;
import com.recruiting.platform.pipeline.joborder.web.dto.JobOrderCreateRequest;
import com.recruiting.platform.pipeline.joborder.web.dto.JobOrderResponse;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Job orders: list, create, read, update (openapi tag Job orders). */
@RestController
@RequestMapping("/api/v1/job-orders")
@PreAuthorize("hasRole('CONSULTANT')")
public class JobOrderController {

    private final JobOrderService service;
    private final JobOrderMapper mapper;

    JobOrderController(JobOrderService service, JobOrderMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    PageResponse<JobOrderResponse> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) UUID accountId, @RequestParam(required = false) List<JobOrderStatus> status,
            @RequestParam(required = false) Boolean open, @RequestParam(required = false) UUID ownerId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        return PageResponse.of(service.list(q, accountId, status, open, ownerId, Math.max(0, page),
                Math.min(100, Math.max(1, size)), sort), mapper::toResponses);
    }

    @PostMapping
    ResponseEntity<JobOrderResponse> create(@Valid @RequestBody JobOrderCreateRequest request) {
        JobOrderResponse r = mapper.toResponse(service.create(request));
        return ResponseEntity.created(URI.create("/api/v1/job-orders/" + r.id())).body(r);
    }

    @GetMapping("/{id}")
    JobOrderResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.get(id));
    }

    @PatchMapping("/{id}")
    JobOrderResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return mapper.toResponse(service.update(id, new Patch(body)));
    }
}
