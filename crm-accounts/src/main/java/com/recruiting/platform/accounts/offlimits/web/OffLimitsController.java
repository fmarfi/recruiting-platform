package com.recruiting.platform.accounts.offlimits.web;

import com.recruiting.platform.accounts.offlimits.app.OffLimitsService;
import com.recruiting.platform.accounts.offlimits.web.dto.OffLimitsRuleCreateRequest;
import com.recruiting.platform.accounts.offlimits.web.dto.OffLimitsRuleResponse;
import com.recruiting.platform.accounts.offlimits.web.dto.OffLimitsRuleUpdateRequest;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
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

/** /off-limits: companies the firm must not recruit from. Everybody reads, MANAGER and ADMIN write. */
@RestController
@RequestMapping("/api/v1/off-limits")
@PreAuthorize("hasRole('CONSULTANT')")
public class OffLimitsController {

    private final OffLimitsService service;
    private final PatchBinder binder;

    public OffLimitsController(OffLimitsService service, PatchBinder binder) {
        this.service = service;
        this.binder = binder;
    }

    @GetMapping
    public PageResponse<OffLimitsRuleResponse> list(@RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return service.list(accountId, active, q, page, size, sort);
    }

    @GetMapping("/{id}")
    public OffLimitsRuleResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<OffLimitsRuleResponse> create(@Valid @RequestBody OffLimitsRuleCreateRequest request) {
        OffLimitsRuleResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/off-limits/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public OffLimitsRuleResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return service.update(id, binder.bind(body, OffLimitsRuleUpdateRequest.class));
    }
}
