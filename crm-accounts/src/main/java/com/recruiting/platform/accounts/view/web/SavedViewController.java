package com.recruiting.platform.accounts.view.web;

import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.view.app.SavedViewService;
import com.recruiting.platform.accounts.view.web.dto.SavedViewCreateRequest;
import com.recruiting.platform.accounts.view.web.dto.SavedViewResponse;
import com.recruiting.platform.accounts.view.web.dto.SavedViewUpdateRequest;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.security.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** /views: saved list views of the data grids (ADR 0010). */
@RestController
@RequestMapping("/api/v1/views")
@PreAuthorize("hasRole('CONSULTANT')")
public class SavedViewController {

    private final SavedViewService service;
    private final PatchBinder binder;
    private final CurrentUser currentUser;

    public SavedViewController(SavedViewService service, PatchBinder binder, CurrentUser currentUser) {
        this.service = service;
        this.binder = binder;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<SavedViewResponse> list(@RequestParam(required = false) ListEntity entity) {
        return service.list(entity, Actor.of(currentUser));
    }

    @PostMapping
    public ResponseEntity<SavedViewResponse> create(@Valid @RequestBody SavedViewCreateRequest request) {
        SavedViewResponse created = service.create(request, Actor.of(currentUser));
        return ResponseEntity.created(URI.create("/api/v1/views/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public SavedViewResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return service.update(id, binder.bind(body, SavedViewUpdateRequest.class), Actor.of(currentUser));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id, Actor.of(currentUser));
    }
}
