package com.recruiting.platform.accounts.search.web;

import com.recruiting.platform.accounts.search.app.SearchService;
import com.recruiting.platform.accounts.search.web.dto.SearchResponse;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.JobOrderStatus;
import com.recruiting.platform.contracts.SearchType;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /search: one search box across accounts, people and job orders (A-14). */
@RestController
@RequestMapping("/api/v1/search")
@PreAuthorize("hasRole('CONSULTANT')")
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping
    public SearchResponse search(@RequestParam String q, @RequestParam(required = false) List<SearchType> types,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) AccountStatus accountStatus,
            @RequestParam(required = false) JobOrderStatus jobOrderStatus,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastContactBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastContactAfter,
            @RequestParam(required = false) Integer limit) {
        return service.search(new SearchService.Query(q, types == null ? null : new LinkedHashSet<>(types), sector,
                accountStatus, jobOrderStatus, ownerId, lastContactBefore, lastContactAfter, limit));
    }
}
