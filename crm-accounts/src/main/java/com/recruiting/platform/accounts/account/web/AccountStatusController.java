package com.recruiting.platform.accounts.account.web;

import com.recruiting.platform.accounts.account.app.AccountStatusService;
import com.recruiting.platform.accounts.account.web.dto.AccountResponse;
import com.recruiting.platform.accounts.account.web.dto.AccountStatusChangeRequest;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** /accounts/{id}/status: the one entry point for account status moves (A-02). */
@RestController
@RequestMapping("/api/v1/accounts/{id}/status")
@PreAuthorize("hasRole('CONSULTANT')")
public class AccountStatusController {

    private final AccountStatusService service;
    private final CurrentUser currentUser;

    public AccountStatusController(AccountStatusService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PatchMapping
    public AccountResponse change(@PathVariable UUID id, @Valid @RequestBody AccountStatusChangeRequest request) {
        return service.change(id, request, Actor.of(currentUser));
    }
}
