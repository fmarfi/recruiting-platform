package com.recruiting.platform.pipeline.agreement.web;

import com.recruiting.platform.contracts.AgreementStatus;
import com.recruiting.platform.pipeline.agreement.app.AgreementMapper;
import com.recruiting.platform.pipeline.agreement.app.AgreementService;
import com.recruiting.platform.pipeline.agreement.web.dto.AgreementResponse;
import com.recruiting.platform.pipeline.shared.dto.PageResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

/** Fee agreements and their signed copy (openapi tag Commercial). */
@RestController
@RequestMapping("/api/v1/agreements")
@PreAuthorize("hasRole('CONSULTANT')")
public class AgreementController {

    private final AgreementService service;
    private final AgreementMapper mapper;

    AgreementController(AgreementService service, AgreementMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    PageResponse<AgreementResponse> list(@RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID jobOrderId, @RequestParam(required = false) List<AgreementStatus> status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        return PageResponse.of(service.list(accountId, jobOrderId, status, Math.max(0, page),
                Math.min(100, Math.max(1, size)), sort), mapper::toResponses);
    }

    @PostMapping
    ResponseEntity<AgreementResponse> create(@RequestBody JsonNode body) {
        AgreementResponse out = mapper.toResponse(service.create(body));
        return ResponseEntity.created(URI.create("/api/v1/agreements/" + out.id())).body(out);
    }

    @GetMapping("/{id}")
    AgreementResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.get(id));
    }

    @PatchMapping("/{id}")
    AgreementResponse update(@PathVariable UUID id, @RequestBody JsonNode body) {
        return mapper.toResponse(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/signed-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    AgreementResponse upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file,
            @RequestParam(value = "version", required = false) Long version) {
        return mapper.toResponse(service.uploadSignedFile(id, file, version));
    }

    @GetMapping("/{id}/signed-file")
    ResponseEntity<byte[]> download(@PathVariable UUID id) {
        AgreementService.Download d = service.download(id);
        String encoded = URLEncoder.encode(d.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .header("X-Content-Type-Options", "nosniff").body(d.bytes());
    }
}
