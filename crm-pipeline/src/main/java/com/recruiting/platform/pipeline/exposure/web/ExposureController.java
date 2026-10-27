package com.recruiting.platform.pipeline.exposure.web;

import com.recruiting.platform.pipeline.exposure.app.ExposureService;
import com.recruiting.platform.pipeline.exposure.web.dto.ExposureResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /people/{id}/exposure: which client accounts saw this person, for which job order and with what outcome. */
@RestController
@RequestMapping("/api/v1/people/{id}/exposure")
@PreAuthorize("hasRole('CONSULTANT')")
public class ExposureController {

    private final ExposureService service;

    ExposureController(ExposureService service) {
        this.service = service;
    }

    @GetMapping
    List<ExposureResponse> list(@PathVariable UUID id, @RequestParam(required = false) UUID accountId) {
        return service.list(id, accountId);
    }
}
