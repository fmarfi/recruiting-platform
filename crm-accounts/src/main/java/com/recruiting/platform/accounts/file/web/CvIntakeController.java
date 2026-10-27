package com.recruiting.platform.accounts.file.web;

import com.recruiting.platform.accounts.file.app.CvIntakeService;
import com.recruiting.platform.accounts.file.app.PersonFileService;
import com.recruiting.platform.accounts.file.web.dto.CvIntakeDtos;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.CvIntakeStatus;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.security.CurrentUser;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** /cv-intake: bulk CV upload and the review queue (B-01). */
@RestController
@RequestMapping("/api/v1/cv-intake")
@PreAuthorize("hasRole('CONSULTANT')")
public class CvIntakeController {

    private final CvIntakeService service;
    private final CurrentUser currentUser;

    public CvIntakeController(CvIntakeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<CvIntakeDtos.Item> list(@RequestParam(required = false) List<CvIntakeStatus> status,
            @RequestParam(required = false) UUID uploadedBy, @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return service.list(status, uploadedBy, batchId, page, size, sort);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvIntakeDtos.Batch> upload(
            @RequestParam(name = "files", required = false) List<MultipartFile> files,
            @RequestParam(name = "sourceChannel", required = false) SourceChannel sourceChannel) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(files, sourceChannel,
                Actor.of(currentUser)));
    }

    @GetMapping("/{id}")
    public CvIntakeDtos.Item get(@PathVariable UUID id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> discard(@PathVariable UUID id) {
        service.discard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID id) {
        PersonFileService.Download d = service.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.fileName(), StandardCharsets.UTF_8).build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff").contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(d.bytes());
    }

    @PostMapping("/{id}/attach")
    public ResponseEntity<CvIntakeDtos.AttachResult> attach(@PathVariable UUID id,
            @Valid @RequestBody CvIntakeDtos.AttachRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.attach(id, request, Actor.of(currentUser)));
    }
}
