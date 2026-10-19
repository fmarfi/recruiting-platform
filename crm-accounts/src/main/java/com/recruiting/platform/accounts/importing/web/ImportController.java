package com.recruiting.platform.accounts.importing.web;

import com.recruiting.platform.accounts.importing.app.ImportCommitService;
import com.recruiting.platform.accounts.importing.app.ImportService;
import com.recruiting.platform.accounts.importing.app.ImportTemplates;
import com.recruiting.platform.accounts.importing.web.dto.ImportJobResponse;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.accounts.shared.Paging;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.ImportKind;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** CSV/XLSX import (A-11): upload and validate, list, read, discard, commit, template. Manager or admin only. */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('MANAGER')")
public class ImportController {

    private static final Map<String, String> SORTS = Map.of("createdAt", "createdAt", "status", "status",
            "kind", "kind");

    private final ImportService service;
    private final ImportCommitService commits;
    private final CurrentUser currentUser;

    public ImportController(ImportService service, ImportCommitService commits, CurrentUser currentUser) {
        this.service = service;
        this.commits = commits;
        this.currentUser = currentUser;
    }

    @GetMapping("/imports")
    public PageResponse<ImportJobResponse> list(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> sort) {
        return service.list(Paging.of(page, size, sort, SORTS, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> upload(@RequestParam ImportKind kind,
            @RequestParam("file") MultipartFile file) throws IOException {
        ImportJobResponse created = service.upload(kind, file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.created(URI.create("/api/v1/imports/" + created.id())).body(created);
    }

    @GetMapping(value = "/import-templates/{kind}", produces = "text/csv")
    public ResponseEntity<String> template(@PathVariable ImportKind kind) {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"" + kind.name().toLowerCase()
                + "-template.csv\"").contentType(MediaType.parseMediaType("text/csv")).body(ImportTemplates.csv(kind));
    }

    @GetMapping("/imports/{id}")
    public ImportJobResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @DeleteMapping("/imports/{id}")
    public ResponseEntity<Void> discard(@PathVariable UUID id) {
        service.discard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/imports/{id}/commit")
    public ImportJobResponse commit(@PathVariable UUID id) {
        return commits.commit(id, Actor.of(currentUser));
    }
}
