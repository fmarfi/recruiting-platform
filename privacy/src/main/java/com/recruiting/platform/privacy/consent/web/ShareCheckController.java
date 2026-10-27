package com.recruiting.platform.privacy.consent.web;

import com.recruiting.platform.pipeline.ShareGuard.ShareCheck;
import com.recruiting.platform.privacy.consent.app.ShareGuardImpl;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ShareCheckResult;
import com.recruiting.platform.privacy.consent.web.dto.ConsentRequests;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /consent/share-check}: which people may be shared with a client (sharing guard, ADR 0011). */
@RestController
@RequestMapping("/api/v1/consent/share-check")
@PreAuthorize("hasRole('CONSULTANT')")
class ShareCheckController {

    private final ShareGuardImpl guard;

    ShareCheckController(ShareGuardImpl guard) {
        this.guard = guard;
    }

    @PostMapping
    List<ShareCheckResult> check(@Valid @RequestBody ConsentRequests.ShareCheck body) {
        return guard.checkAll(body.personIds()).values().stream().map(ShareCheckController::map).toList();
    }

    private static ShareCheckResult map(ShareCheck c) {
        return new ShareCheckResult(c.personId(), c.shareable(), c.reason());
    }
}
