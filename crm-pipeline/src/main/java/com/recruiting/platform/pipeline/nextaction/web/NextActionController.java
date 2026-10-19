package com.recruiting.platform.pipeline.nextaction.web;

import com.recruiting.platform.pipeline.nextaction.app.NextActionFilter;
import com.recruiting.platform.pipeline.nextaction.app.NextActionMapper;
import com.recruiting.platform.pipeline.nextaction.app.NextActionService;
import com.recruiting.platform.pipeline.nextaction.web.dto.NextActionCreateRequest;
import com.recruiting.platform.pipeline.nextaction.web.dto.NextActionResponse;
import com.recruiting.platform.pipeline.shared.Actor;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
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

/** Next actions: list, create, change or complete, delete (openapi tag Next actions). */
@RestController
@RequestMapping("/api/v1/next-actions")
@PreAuthorize("hasRole('CONSULTANT')")
public class NextActionController {

    private final NextActionService service;
    private final NextActionMapper mapper;

    NextActionController(NextActionService service, NextActionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    PageResponse<NextActionResponse> list(@RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) UUID ownerId, @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID personId, @RequestParam(required = false) UUID jobOrderId,
            @RequestParam(required = false) Boolean done,
            @RequestParam(required = false) LocalDate dueBefore, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) List<String> sort,
            Authentication auth) {
        NextActionFilter f = new NextActionFilter(mine, ownerId, accountId, personId, jobOrderId, done, dueBefore);
        return PageResponse.of(service.list(f, Actor.of(auth), Math.max(0, page),
                Math.min(100, Math.max(1, size)), sort), mapper::toResponses);
    }

    @PostMapping
    ResponseEntity<NextActionResponse> create(@Valid @RequestBody NextActionCreateRequest r, Authentication auth) {
        NextActionResponse out = mapper.toResponse(service.create(r, Actor.of(auth)));
        return ResponseEntity.created(URI.create("/api/v1/next-actions/" + out.id())).body(out);
    }

    @PatchMapping("/{id}")
    NextActionResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return mapper.toResponse(service.update(id, new Patch(body)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
