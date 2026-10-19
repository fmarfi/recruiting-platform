package com.recruiting.platform.accounts.person.web;

import com.recruiting.platform.accounts.person.app.PersonQueryService;
import com.recruiting.platform.accounts.person.app.PersonService;
import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import com.recruiting.platform.accounts.person.web.dto.PersonResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonUpdateRequest;
import com.recruiting.platform.accounts.shared.ActorResolver;
import com.recruiting.platform.accounts.shared.Paging;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

/** /people: network list, add, view and edit (A-12). */
@RestController
@RequestMapping("/api/v1/people")
@PreAuthorize("hasRole('CONSULTANT')")
public class PersonController {

    private static final Map<String, String> SORTS = Map.of("name", "name", "company", "company",
            "lastContactAt", "lastContactAt", "createdAt", "createdAt");

    private final PersonService service;
    private final PersonQueryService queries;
    private final PatchBinder binder;
    private final ActorResolver actors;

    public PersonController(PersonService service, PersonQueryService queries, PatchBinder binder,
            ActorResolver actors) {
        this.service = service;
        this.queries = queries;
        this.binder = binder;
        this.actors = actors;
    }

    @GetMapping
    public PageResponse<PersonSummaryResponse> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) List<PersonType> type, @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) List<ConsentStatus> consentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastContactBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastContactAfter,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return queries.list(q, type, ownerId, consentStatus, lastContactBefore, lastContactAfter,
                Paging.of(page, size, sort, SORTS, Sort.by("name")));
    }

    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonCreateRequest request) {
        PersonResponse created = service.create(request, actors.current());
        return ResponseEntity.created(URI.create("/api/v1/people/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public PersonResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public PersonResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return service.update(id, binder.bind(body, PersonUpdateRequest.class), actors.current());
    }
}
