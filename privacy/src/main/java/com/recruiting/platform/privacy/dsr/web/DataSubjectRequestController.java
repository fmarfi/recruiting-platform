package com.recruiting.platform.privacy.dsr.web;

import com.recruiting.platform.contracts.DataSubjectRequestStatus;
import com.recruiting.platform.contracts.DataSubjectRequestType;
import com.recruiting.platform.privacy.dsr.app.DataSubjectRequestService;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.DsrCreate;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.DsrResponse;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.DsrUpdate;
import com.recruiting.platform.security.ApiPage;
import com.recruiting.platform.security.ApiPageable;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
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

/** Tag Data-subject requests: log, list and handle requests (anyone logs, managers handle). */
@RestController
@RequestMapping("/api/v1/data-subject-requests")
class DataSubjectRequestController {

    private static final Map<String, String> SORTABLE = Map.of("receivedAt", "receivedAt", "dueOn", "dueOn",
            "status", "status", "type", "type", "createdAt", "createdAt");

    private final DataSubjectRequestService service;

    DataSubjectRequestController(DataSubjectRequestService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    ApiPage<DsrResponse> list(@RequestParam(required = false) List<DataSubjectRequestStatus> status,
            @RequestParam(required = false) DataSubjectRequestType type,
            @RequestParam(required = false) UUID personId, @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        var pageable = ApiPageable.of(page, size, sort, SORTABLE, Sort.by("dueOn"));
        return ApiPage.of(service.list(status, type, personId, overdue, pageable), r -> r);
    }

    @PostMapping
    @PreAuthorize("hasRole('CONSULTANT')")
    ResponseEntity<DsrResponse> create(@Valid @RequestBody DsrCreate body) {
        DsrResponse created = service.create(body);
        return ResponseEntity.created(URI.create("/api/v1/data-subject-requests/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    DsrResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    DsrResponse update(@PathVariable UUID id, @Valid @RequestBody DsrUpdate body) {
        return service.update(id, body);
    }
}
