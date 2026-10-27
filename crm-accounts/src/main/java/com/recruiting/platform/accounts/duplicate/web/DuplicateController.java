package com.recruiting.platform.accounts.duplicate.web;

import com.recruiting.platform.accounts.duplicate.app.DuplicateService;
import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateCandidateResponse;
import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateCheckRequest;
import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateMatchResponse;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.DuplicateStatus;
import com.recruiting.platform.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** /duplicates: check before create, the queue, dismiss (B-04). Merge is orchestrated by privacy. */
@RestController
@RequestMapping("/api/v1/duplicates")
@PreAuthorize("hasRole('CONSULTANT')")
public class DuplicateController {

    private final DuplicateService service;
    private final CurrentUser currentUser;

    public DuplicateController(DuplicateService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/check")
    public List<DuplicateMatchResponse> check(@Valid @RequestBody DuplicateCheckRequest request) {
        if (request.isEmpty()) {
            throw PatchBinder.validation("body", "REQUIRED", "send at least one member");
        }
        return service.check(request);
    }

    @GetMapping
    public PageResponse<DuplicateCandidateResponse> list(@RequestParam(required = false) List<DuplicateStatus> status,
            @RequestParam(required = false) UUID personId, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> sort) {
        return service.list(status, personId, page, size, sort);
    }

    @PostMapping("/{id}/dismiss")
    public DuplicateCandidateResponse dismiss(@PathVariable UUID id) {
        return service.dismiss(id, currentUser.id());
    }
}
