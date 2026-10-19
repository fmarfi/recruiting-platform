package com.recruiting.platform.pipeline.application.web;

import com.recruiting.platform.contracts.PipelineStage;
import com.recruiting.platform.pipeline.application.app.ApplicationMapper;
import com.recruiting.platform.pipeline.application.app.ApplicationService;
import com.recruiting.platform.pipeline.application.web.dto.ApplicationCreateRequest;
import com.recruiting.platform.pipeline.application.web.dto.ApplicationResponse;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.PageResponse;
import com.recruiting.platform.pipeline.shared.dto.PersonSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Board of a job order and candidate suggestions (openapi tag Pipeline). */
@RestController
@RequestMapping("/api/v1/job-orders/{id}")
@PreAuthorize("hasRole('CONSULTANT')")
public class JobOrderApplicationController {

    private final ApplicationService service;
    private final ApplicationMapper mapper;
    private final Refs refs;

    JobOrderApplicationController(ApplicationService service, ApplicationMapper mapper, Refs refs) {
        this.service = service;
        this.mapper = mapper;
        this.refs = refs;
    }

    @GetMapping("/applications")
    List<ApplicationResponse> list(@PathVariable UUID id, @RequestParam(required = false) List<PipelineStage> stage) {
        return mapper.toResponses(service.listForJobOrder(id, stage));
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    ApplicationResponse add(@PathVariable UUID id, @Valid @RequestBody ApplicationCreateRequest r,
            Authentication auth) {
        return mapper.toResponse(service.add(id, r.personId(), Actor.of(auth)));
    }

    @GetMapping("/candidate-suggestions")
    PageResponse<PersonSummary> suggestions(@PathVariable UUID id, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(service.suggestions(id, q, Math.max(0, page), Math.min(100, Math.max(1, size))),
                infos -> {
                    Refs.Lookup l = refs.load(List.of(), infos.stream().map(i -> i.id()).toList(), List.of());
                    return infos.stream().map(i -> l.person(i.id())).toList();
                });
    }
}
