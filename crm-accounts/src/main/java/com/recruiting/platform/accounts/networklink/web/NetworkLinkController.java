package com.recruiting.platform.accounts.networklink.web;

import com.recruiting.platform.accounts.networklink.app.NetworkLinkService;
import com.recruiting.platform.accounts.networklink.web.dto.NetworkLinkCreateRequest;
import com.recruiting.platform.accounts.networklink.web.dto.NetworkLinkResponse;
import com.recruiting.platform.accounts.networklink.web.dto.NetworkLinkUpdateRequest;
import com.recruiting.platform.accounts.shared.PatchBinder;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** /network-links: who knows or introduced whom (A-13). */
@RestController
@RequestMapping("/api/v1/network-links")
@PreAuthorize("hasRole('CONSULTANT')")
public class NetworkLinkController {

    private final NetworkLinkService service;
    private final PatchBinder binder;

    public NetworkLinkController(NetworkLinkService service, PatchBinder binder) {
        this.service = service;
        this.binder = binder;
    }

    @PostMapping
    public ResponseEntity<NetworkLinkResponse> create(@Valid @RequestBody NetworkLinkCreateRequest request) {
        NetworkLinkResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/network-links/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public NetworkLinkResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public NetworkLinkResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return service.update(id, binder.bind(body, NetworkLinkUpdateRequest.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
