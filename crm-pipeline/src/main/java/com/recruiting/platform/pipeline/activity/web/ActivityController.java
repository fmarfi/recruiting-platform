package com.recruiting.platform.pipeline.activity.web;

import com.recruiting.platform.contracts.ActivityType;
import com.recruiting.platform.pipeline.activity.app.ActivityFilter;
import com.recruiting.platform.pipeline.activity.app.ActivityMapper;
import com.recruiting.platform.pipeline.activity.app.ActivityService;
import com.recruiting.platform.pipeline.activity.web.dto.ActivityCreateRequest;
import com.recruiting.platform.pipeline.activity.web.dto.ActivityResponse;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Activities: list, quick add, read, correct, delete (openapi tag Activities). */
@RestController
@RequestMapping("/api/v1/activities")
@PreAuthorize("hasRole('CONSULTANT')")
public class ActivityController {

    private final ActivityService service;
    private final ActivityMapper mapper;

    ActivityController(ActivityService service, ActivityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    PageResponse<ActivityResponse> list(@RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID personId, @RequestParam(required = false) UUID jobOrderId,
            @RequestParam(required = false) List<ActivityType> type, @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        ActivityFilter f = new ActivityFilter(accountId, personId, jobOrderId, type, userId, from, to);
        return PageResponse.of(service.list(f, Math.max(0, page), Math.min(100, Math.max(1, size)), sort),
                mapper::toResponses);
    }

    @PostMapping
    ResponseEntity<ActivityResponse> log(@Valid @RequestBody ActivityCreateRequest r, Authentication auth) {
        ActivityResponse out = mapper.toResponse(service.log(r, Actor.of(auth)));
        return ResponseEntity.created(URI.create("/api/v1/activities/" + out.id())).body(out);
    }

    @GetMapping("/{id}")
    ActivityResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.get(id));
    }

    @PatchMapping("/{id}")
    ActivityResponse update(@PathVariable UUID id, @RequestBody JsonNode body, Authentication auth) {
        return mapper.toResponse(service.update(id, new Patch(body), Actor.of(auth)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        service.delete(id, Actor.of(auth));
        return ResponseEntity.noContent().build();
    }
}
