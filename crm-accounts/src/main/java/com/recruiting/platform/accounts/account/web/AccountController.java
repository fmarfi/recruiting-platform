package com.recruiting.platform.accounts.account.web;

import com.recruiting.platform.accounts.account.app.AccountQueryService;
import com.recruiting.platform.accounts.account.app.AccountService;
import com.recruiting.platform.accounts.account.web.dto.AccountCreateRequest;
import com.recruiting.platform.accounts.account.web.dto.AccountResponse;
import com.recruiting.platform.accounts.account.web.dto.AccountUpdateRequest;
import com.recruiting.platform.accounts.shared.ActorResolver;
import com.recruiting.platform.accounts.shared.Paging;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.AccountStatus;
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
import tools.jackson.databind.JsonNode;

/** /accounts: overview, add, header and edit (A-01). */
@RestController
@RequestMapping("/api/v1/accounts")
@PreAuthorize("hasRole('CONSULTANT')")
public class AccountController {

    private static final Map<String, String> SORTS = Map.of("name", "name", "status", "status",
            "openedOn", "openedOn", "lastActivityAt", "lastActivityAt", "openJobCount", "openJobCount");

    private final AccountService service;
    private final AccountQueryService queries;
    private final PatchBinder binder;
    private final ActorResolver actors;

    public AccountController(AccountService service, AccountQueryService queries, PatchBinder binder,
            ActorResolver actors) {
        this.service = service;
        this.queries = queries;
        this.binder = binder;
        this.actors = actors;
    }

    @GetMapping
    public PageResponse<AccountResponse> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) List<AccountStatus> status,
            @RequestParam(required = false) String sector, @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return queries.list(q, status, sector, ownerId, Paging.of(page, size, sort, SORTS, Sort.by("name")));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        AccountResponse created = service.create(request, actors.current());
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public AccountResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return service.update(id, binder.bind(body, AccountUpdateRequest.class), actors.current());
    }
}
