package com.recruiting.platform.accounts.person.web;

import com.recruiting.platform.accounts.person.app.PersonExportService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /people/{id}/export: everything stored about a person as JSON (S-04), MANAGER and ADMIN. */
@RestController
@RequestMapping("/api/v1/people/{id}/export")
@PreAuthorize("hasRole('MANAGER')")
public class PersonExportController {

    private final PersonExportService service;

    public PersonExportController(PersonExportService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> export(@PathVariable UUID id, HttpServletRequest request) {
        Map<String, Object> body = service.export(id, request.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"person-" + id + ".json\"")
                .body(body);
    }
}
