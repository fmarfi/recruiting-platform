package com.recruiting.platform.pipeline.proposal.web;

import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.pipeline.proposal.app.ProposalMapper;
import com.recruiting.platform.pipeline.proposal.app.ProposalService;
import com.recruiting.platform.pipeline.proposal.web.dto.ProposalCreateRequest;
import com.recruiting.platform.pipeline.proposal.web.dto.ProposalResponse;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Proposals: list, create, read, edit (openapi tag Proposals). */
@RestController
@RequestMapping("/api/v1/proposals")
@PreAuthorize("hasRole('CONSULTANT')")
public class ProposalController {

    private final ProposalService service;
    private final ProposalMapper mapper;

    ProposalController(ProposalService service, ProposalMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    PageResponse<ProposalResponse> list(@RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) List<ProposalStatus> status,
            @RequestParam(required = false) UUID ownerId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) List<String> sort) {
        return PageResponse.of(service.list(accountId, status, ownerId, Math.max(0, page),
                Math.min(100, Math.max(1, size)), sort), mapper::toResponses);
    }

    @PostMapping
    ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalCreateRequest r, Authentication auth) {
        ProposalResponse out = mapper.toResponse(service.create(r, Actor.of(auth)));
        return ResponseEntity.created(URI.create("/api/v1/proposals/" + out.id())).body(out);
    }

    @GetMapping("/{id}")
    ProposalResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.get(id));
    }

    @PatchMapping("/{id}")
    ProposalResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return mapper.toResponse(service.update(id, new Patch(body)));
    }
}
