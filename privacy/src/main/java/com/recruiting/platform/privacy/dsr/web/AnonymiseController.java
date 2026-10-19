package com.recruiting.platform.privacy.dsr.web;

import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.privacy.dsr.app.PersonErasureService;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.AnonymiseBody;
import com.recruiting.platform.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** POST /people/{id}/anonymise: irreversible, admin only. The reason is part of the audit trail of the request. */
@RestController
@RequestMapping("/api/v1/people/{id}/anonymise")
class AnonymiseController {

    private final PersonErasureService erasure;
    private final CurrentUser currentUser;

    AnonymiseController(PersonErasureService erasure, CurrentUser currentUser) {
        this.erasure = erasure;
        this.currentUser = currentUser;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void anonymise(@PathVariable UUID id, @Valid @RequestBody AnonymiseBody body) {
        erasure.erase(id, RetentionAction.ANONYMISE, currentUser.id());
    }
}
