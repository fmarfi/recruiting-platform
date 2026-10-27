package com.recruiting.platform.accounts.file.web;

import com.recruiting.platform.accounts.file.app.PersonFileService;
import com.recruiting.platform.accounts.person.web.dto.PersonFileResponse;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.security.CurrentUser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

/** /people/{id}/files: CVs and documents (B-01). */
@RestController
@RequestMapping("/api/v1/people/{id}/files")
@PreAuthorize("hasRole('CONSULTANT')")
public class PersonFileController {

    private final PersonFileService service;
    private final CurrentUser currentUser;

    public PersonFileController(PersonFileService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<PersonFileResponse> list(@PathVariable UUID id) {
        return service.list(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PersonFileResponse> upload(@PathVariable UUID id,
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "kind", required = false) PersonFileKind kind) {
        PersonFileResponse created = service.upload(id, file, kind, Actor.of(currentUser));
        return ResponseEntity.created(URI.create("/api/v1/people/" + id + "/files/" + created.id())).body(created);
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, @PathVariable UUID fileId) {
        PersonFileService.Download d = service.download(id, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.fileName(), StandardCharsets.UTF_8).build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff").contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(d.bytes());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @PathVariable UUID fileId) {
        service.delete(id, fileId);
        return ResponseEntity.noContent().build();
    }
}
