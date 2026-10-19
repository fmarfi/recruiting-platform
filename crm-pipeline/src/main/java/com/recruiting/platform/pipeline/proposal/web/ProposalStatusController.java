package com.recruiting.platform.pipeline.proposal.web;

import com.recruiting.platform.pipeline.proposal.app.ProposalMapper;
import com.recruiting.platform.pipeline.proposal.app.ProposalStatusService;
import com.recruiting.platform.pipeline.proposal.web.dto.ProposalResponse;
import com.recruiting.platform.pipeline.proposal.web.dto.ProposalStatusChangeRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PATCH /proposals/{id}/status. */
@RestController
@RequestMapping("/api/v1/proposals/{id}/status")
@PreAuthorize("hasRole('CONSULTANT')")
public class ProposalStatusController {

    private final ProposalStatusService service;
    private final ProposalMapper mapper;

    ProposalStatusController(ProposalStatusService service, ProposalMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PatchMapping
    ProposalResponse change(@PathVariable UUID id, @Valid @RequestBody ProposalStatusChangeRequest r) {
        return mapper.toResponse(service.change(id, r.status(), r.reason(), r.version()));
    }
}
