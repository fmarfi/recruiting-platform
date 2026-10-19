package com.recruiting.platform.accounts.contact.web;

import com.recruiting.platform.accounts.contact.app.ContactService;
import com.recruiting.platform.accounts.contact.web.dto.ContactCreateRequest;
import com.recruiting.platform.accounts.contact.web.dto.ContactResponse;
import com.recruiting.platform.accounts.contact.web.dto.ContactUpdateRequest;
import com.recruiting.platform.accounts.shared.ActorResolver;
import com.recruiting.platform.accounts.shared.PatchBinder;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** /accounts/{id}/contacts: contacts of one account (A-03). */
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/contacts")
@PreAuthorize("hasRole('CONSULTANT')")
public class ContactController {

    private final ContactService service;
    private final PatchBinder binder;
    private final ActorResolver actors;

    public ContactController(ContactService service, PatchBinder binder, ActorResolver actors) {
        this.service = service;
        this.binder = binder;
        this.actors = actors;
    }

    @GetMapping
    public List<ContactResponse> list(@PathVariable UUID accountId,
            @RequestParam(defaultValue = "true") boolean active) {
        return service.listForAccount(accountId, active);
    }

    @PostMapping
    public ResponseEntity<ContactResponse> add(@PathVariable UUID accountId,
            @Valid @RequestBody ContactCreateRequest request) {
        ContactResponse created = service.add(accountId, request, actors.current());
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + accountId + "/contacts/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{contactId}")
    public ContactResponse update(@PathVariable UUID accountId, @PathVariable UUID contactId,
            @RequestBody JsonNode body) {
        return service.update(accountId, contactId, binder.bind(body, ContactUpdateRequest.class));
    }

    @DeleteMapping("/{contactId}")
    public ContactResponse end(@PathVariable UUID accountId, @PathVariable UUID contactId) {
        return service.end(accountId, contactId, actors.current());
    }
}
