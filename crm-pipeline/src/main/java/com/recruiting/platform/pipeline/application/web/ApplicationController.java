package com.recruiting.platform.pipeline.application.web;

import com.recruiting.platform.pipeline.application.app.ApplicationMapper;
import com.recruiting.platform.pipeline.application.app.ApplicationService;
import com.recruiting.platform.pipeline.application.app.ApplicationStageService;
import com.recruiting.platform.pipeline.application.web.dto.ApplicationResponse;
import com.recruiting.platform.pipeline.application.web.dto.StageChangeRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** One application: read, remove (SOURCED only), move to another stage, and the applications of a person. */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('CONSULTANT')")
public class ApplicationController {

    private final ApplicationService service;
    private final ApplicationStageService stages;
    private final ApplicationMapper mapper;

    ApplicationController(ApplicationService service, ApplicationStageService stages, ApplicationMapper mapper) {
        this.service = service;
        this.stages = stages;
        this.mapper = mapper;
    }

    @GetMapping("/applications/{id}")
    ApplicationResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.get(id));
    }

    @DeleteMapping("/applications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID id) {
        service.remove(id);
    }

    @PatchMapping("/applications/{id}/stage")
    ApplicationResponse changeStage(@PathVariable UUID id, @Valid @RequestBody StageChangeRequest r) {
        return mapper.toResponse(stages.change(id, r.stage(), r.reason(), r.version()));
    }

    @GetMapping("/people/{id}/applications")
    List<ApplicationResponse> ofPerson(@PathVariable UUID id) {
        return mapper.toResponses(service.listForPerson(id));
    }
}
