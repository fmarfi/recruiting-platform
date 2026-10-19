package com.recruiting.platform.privacy.notice.web;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.privacy.notice.app.PrivacyNoticeService;
import com.recruiting.platform.privacy.notice.web.dto.NoticeCreateRequest;
import com.recruiting.platform.privacy.notice.web.dto.NoticeResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tag Privacy notices. */
@RestController
@RequestMapping("/api/v1/privacy-notices")
class PrivacyNoticeController {

    private final PrivacyNoticeService notices;

    PrivacyNoticeController(PrivacyNoticeService notices) {
        this.notices = notices;
    }

    @GetMapping
    @PreAuthorize("hasRole('CONSULTANT')")
    List<NoticeResponse> list(@RequestParam(required = false) Language language,
            @RequestParam(defaultValue = "false") boolean current) {
        return notices.list(language, current);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<NoticeResponse> create(@Valid @RequestBody NoticeCreateRequest body) {
        NoticeResponse created = notices.create(body);
        return ResponseEntity.created(URI.create("/api/v1/privacy-notices/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONSULTANT')")
    NoticeResponse get(@PathVariable UUID id) {
        return notices.get(id);
    }
}
