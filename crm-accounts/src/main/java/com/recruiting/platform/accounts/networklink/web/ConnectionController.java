package com.recruiting.platform.accounts.networklink.web;

import com.recruiting.platform.accounts.networklink.app.ConnectionQueryService;
import com.recruiting.platform.accounts.networklink.web.dto.AccountConnectionResponse;
import com.recruiting.platform.accounts.networklink.web.dto.PersonConnectionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connection lists: /people/{id}/links and /accounts/{id}/connections (A-13). */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('CONSULTANT')")
public class ConnectionController {

    private final ConnectionQueryService queries;

    public ConnectionController(ConnectionQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/people/{id}/links")
    public List<PersonConnectionResponse> personLinks(@PathVariable UUID id) {
        return queries.forPerson(id);
    }

    @GetMapping("/accounts/{id}/connections")
    public List<AccountConnectionResponse> accountConnections(@PathVariable UUID id) {
        return queries.forAccount(id);
    }
}
