package com.recruiting.platform.accounts.referencedata.web;

import com.recruiting.platform.accounts.referencedata.app.ReferenceDataService;
import com.recruiting.platform.accounts.referencedata.web.dto.ReferenceDataResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /reference-data: lifecycles, vocabularies and sectors for the UI. */
@RestController
@RequestMapping("/api/v1/reference-data")
@PreAuthorize("hasRole('CONSULTANT')")
public class ReferenceDataController {

    private final ReferenceDataService service;

    public ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping
    public ReferenceDataResponse get() {
        return service.get();
    }
}
